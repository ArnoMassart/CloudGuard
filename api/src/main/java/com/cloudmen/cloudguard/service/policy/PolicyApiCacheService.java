package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.service.GoogleDirectoryFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.OrgUnit;
import com.google.api.services.admin.directory.model.OrgUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Shared cache for Cloud Identity Policy API responses. Fetches all policies
 * once and caches them for 5 minutes to stay within the 30 req/min quota.
 */
@Service
public class PolicyApiCacheService {
    private static final Logger log = LoggerFactory.getLogger(PolicyApiCacheService.class);
    private static final String POLICY_API_SCOPE = "https://www.googleapis.com/auth/cloud-identity.policies.readonly";
    private static final String POLICY_API_BASE = "https://cloudidentity.googleapis.com/v1/policies";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final GoogleDirectoryFactory directoryFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile List<JsonNode> cachedPolicies;
    private volatile long policyCacheTimestamp;

    private volatile Map<String, String> cachedOuIdToPath;
    private volatile long ouCacheTimestamp;

    public PolicyApiCacheService(GoogleDirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    public List<JsonNode> getAllPolicies(String adminEmail) throws Exception {
        long now = System.currentTimeMillis();
        if (cachedPolicies != null && (now - policyCacheTimestamp) < CACHE_TTL_MS) {
            log.debug("Returning {} cached policies", cachedPolicies.size());
            return cachedPolicies;
        }
        List<JsonNode> fresh = fetchAllPolicies(adminEmail);
        cachedPolicies = fresh;
        policyCacheTimestamp = now;
        return fresh;
    }

    /**
     * Returns a cached mapping from OU ID (e.g. "03ph8a2z2rzxpai") to OU path (e.g. "/Sales/TeamA").
     * Root OU "/" is always present.
     */
    public Map<String, String> getOuIdToPathMap(String adminEmail) throws Exception {
        long now = System.currentTimeMillis();
        if (cachedOuIdToPath != null && (now - ouCacheTimestamp) < CACHE_TTL_MS) {
            return cachedOuIdToPath;
        }
        Map<String, String> fresh = fetchOuIdToPathMap(adminEmail);
        cachedOuIdToPath = fresh;
        ouCacheTimestamp = now;
        return fresh;
    }

    /**
     * Resolves "orgUnits/xxxx" (as returned by Policy API) to an OU path like "/Sales".
     * Returns "/" if the ID is empty, absent, or cannot be resolved.
     */
    public String resolveOuIdToPath(String policyOuRef, Map<String, String> ouMap) {
        if (policyOuRef == null || policyOuRef.isBlank()) return "/";
        String id = policyOuRef.startsWith("orgUnits/") ? policyOuRef.substring("orgUnits/".length()) : policyOuRef;
        return ouMap.getOrDefault(id, "/");
    }

    private Map<String, String> fetchOuIdToPathMap(String adminEmail) throws Exception {
        Directory directory = directoryFactory.getDirectoryService(
                Set.of(DirectoryScopes.ADMIN_DIRECTORY_ORGUNIT_READONLY), adminEmail);
        OrgUnits response = directory.orgunits().list("my_customer").setType("all").execute();

        Map<String, String> map = new HashMap<>();
        if (response != null && response.getOrganizationUnits() != null) {
            for (OrgUnit ou : response.getOrganizationUnits()) {
                if (ou.getOrgUnitId() != null && ou.getOrgUnitPath() != null) {
                    map.put(ou.getOrgUnitId(), ou.getOrgUnitPath());
                }
            }
        }
        log.info("Loaded {} OU ID→path mappings (cached for {}s)", map.size(), CACHE_TTL_MS / 1000);
        return map;
    }

    private List<JsonNode> fetchAllPolicies(String adminEmail) throws Exception {
        var creds = directoryFactory.getCredentials(Set.of(POLICY_API_SCOPE), adminEmail);
        creds.refreshIfExpired();
        String token = creds.getAccessToken().getTokenValue();

        List<JsonNode> out = new ArrayList<>();
        String pageToken = null;

        try {
            do {
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(POLICY_API_BASE)
                        .queryParam("pageSize", 100);
                if (pageToken != null) builder.queryParam("pageToken", pageToken);
                URI uri = builder.build().encode().toUri();

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);

                ResponseEntity<String> resp = restTemplate.exchange(
                        uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

                if (resp.getBody() == null) break;
                JsonNode root = mapper.readTree(resp.getBody());
                JsonNode policiesNode = root.get("policies");
                if (policiesNode != null && policiesNode.isArray()) {
                    for (JsonNode p : policiesNode) out.add(p);
                }

                JsonNode next = root.get("nextPageToken");
                pageToken = (next != null && !next.isNull() && !next.asText().isBlank()) ? next.asText() : null;
            } while (pageToken != null);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Policy API fetch failed: HTTP " +
                    e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        log.info("Fetched {} policies from Policy API (cached for {}s)", out.size(), CACHE_TTL_MS / 1000);
        return out;
    }
}
