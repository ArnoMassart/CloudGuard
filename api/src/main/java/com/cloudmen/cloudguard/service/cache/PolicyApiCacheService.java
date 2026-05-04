package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.policy.PolicyApiCacheEntry;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.google.api.services.admin.directory.model.OrgUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Shared cache for Cloud Identity Policy API responses and Directory OU id→path maps,
 * per Workspace admin, via Caffeine (15 min expiry; Policy API quota aware).
 */
@Service
public class PolicyApiCacheService extends AbstractGoogleWorkspaceCacheService<PolicyApiCacheEntry> {
    private static final Logger log = LoggerFactory.getLogger(PolicyApiCacheService.class);
    private static final String POLICY_API_SCOPE = "https://www.googleapis.com/auth/cloud-identity.policies.readonly";
    private static final String POLICY_API_BASE = "https://cloudidentity.googleapis.com/v1/policies";
    private static final int CACHE_TTL_MINUTES = 15;

    private final GoogleApiFactory directoryFactory;
    private final MessageSource messageSource;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public PolicyApiCacheService(
            GoogleApiFactory directoryFactory,
            @Qualifier("messageSource") MessageSource messageSource,
            UserService userService,
            OrganizationService organizationService) {
        super(userService, organizationService, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        this.directoryFactory = directoryFactory;
        this.messageSource = messageSource;
    }

    @Override
    protected PolicyApiCacheEntry fetchFromGoogle(String adminEmail, PolicyApiCacheEntry fallback) {
        try {
            List<JsonNode> policies = List.copyOf(fetchAllPolicies(adminEmail));
            Map<String, String> ouMap = Map.copyOf(fetchOuIdToPathMap(adminEmail));
            return new PolicyApiCacheEntry(policies, ouMap);
        } catch (Exception e) {
            if (fallback != null) {
                log.error("Policy API / Directory failed; using stale cache: {}", e.getMessage());
                return fallback;
            }
            throw new GoogleWorkspaceSyncException(
                    "Fout bij ophalen policy / OU data: " + e.getMessage(), e);
        }
    }

    public List<JsonNode> getAllPolicies(String adminEmail) {
        return getOrFetchDataByAdmin(adminEmail).policies();
    }

    public Map<String, String> getOuIdToPathMap(String adminEmail) {
        return getOrFetchDataByAdmin(adminEmail).ouIdToPath();
    }

    /**
     * Resolves "orgUnits/xxxx" (as returned by Policy API) to an OU path like "/Sales".
     */
    public String resolveOuIdToPath(String policyOuRef, Map<String, String> ouMap) {
        if (policyOuRef == null || policyOuRef.isBlank()) return "/";
        String id = policyOuRef.startsWith("orgUnits/") ? policyOuRef.substring("orgUnits/".length()) : policyOuRef;
        return ouMap.getOrDefault(id, "/");
    }

    /**
     * Resolves an OU path (e.g. "/" or "/Sales") to the org unit ID for Chrome Policy API.
     * Returns null if the path is not found.
     */
    public String resolvePathToOuId(String orgUnitPath, Map<String, String> ouMap) {
        if (orgUnitPath == null || orgUnitPath.isBlank()) orgUnitPath = "/";
        String path = orgUnitPath.trim();
        for (Map.Entry<String, String> e : ouMap.entrySet()) {
            String p = e.getValue();
            if (p == null) continue;
            String norm = p.trim();
            if (norm.equals(path) || (path.equals("/") && (norm.equals("/") || norm.isEmpty()))) {
                return e.getKey();
            }
        }
        return null;
    }

    private Map<String, String> fetchOuIdToPathMap(String adminEmail) throws Exception {
        Directory directory = directoryFactory.getDirectoryService(
                Set.of(DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT_READONLY), adminEmail);
        OrgUnits response = directory.orgunits().list("my_customer").setType("ALL_INCLUDING_PARENT").execute();

        Map<String, String> map = new HashMap<>();
        if (response != null && response.getOrganizationUnits() != null) {
            for (OrgUnit ou : response.getOrganizationUnits()) {
                if (ou.getOrgUnitId() != null && ou.getOrgUnitPath() != null) {
                    map.put(ou.getOrgUnitId(), ou.getOrgUnitPath());
                }
            }
        }
        log.info("Loaded {} OU ID→path mappings (Caffeine TTL {} min)", map.size(), CACHE_TTL_MINUTES);
        return map;
    }

    private List<JsonNode> fetchAllPolicies(String adminEmail) throws Exception {
        var creds = directoryFactory.getCredentials(Set.of(POLICY_API_SCOPE), adminEmail);
        creds.refreshIfExpired();
        String token = creds.getAccessToken().getTokenValue();

        List<JsonNode> out = new ArrayList<>();
        String pageToken = null;

        do {
            int maxRetries = 3;
            ResponseEntity<String> resp = null;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(POLICY_API_BASE)
                            .queryParam("pageSize", 100);
                    if (pageToken != null) builder.queryParam("pageToken", pageToken);
                    URI uri = builder.build().encode().toUri();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);

                    resp = restTemplate.exchange(
                            uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                    break;
                } catch (HttpStatusCodeException e) {
                    if (e.getStatusCode().value() == 429 && attempt < maxRetries) {
                        long backoff = (long) Math.pow(2, attempt + 1) * 1000L;
                        log.warn("Policy API 429 rate-limited, retrying in {}ms (attempt {}/{})", backoff, attempt + 1, maxRetries);
                        Thread.sleep(backoff);
                    } else {
                        log.error(
                                "Policy API fetch failed: HTTP {} (response body omitted from logs if large)",
                                e.getStatusCode(), e);
                        throw new GoogleWorkspaceSyncException(
                                messageSource.getMessage(
                                        "api.google.policy_api_fetch_failed", null, LocaleContextHolder.getLocale()),
                                e);
                    }
                }
            }

            if (resp == null || resp.getBody() == null) break;
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode policiesNode = root.get("policies");
            if (policiesNode != null && policiesNode.isArray()) {
                for (JsonNode p : policiesNode) out.add(p);
            }

            JsonNode next = root.get("nextPageToken");
            pageToken = (next != null && !next.isNull() && !next.asText().isBlank()) ? next.asText() : null;
        } while (pageToken != null);

        log.info("Fetched {} policies from Policy API (Caffeine TTL {} min)", out.size(), CACHE_TTL_MINUTES);
        return out;
    }
}
