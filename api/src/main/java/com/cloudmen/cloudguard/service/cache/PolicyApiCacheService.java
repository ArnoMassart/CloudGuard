package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.utility.GoogleApiFactory;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared cache for Cloud Identity Policy API responses. Fetches all policies
 * once and caches them for 5 minutes to stay within the 30 req/min quota.
 */
@Service
public class PolicyApiCacheService {
    private static final Logger log = LoggerFactory.getLogger(PolicyApiCacheService.class);
    private static final String POLICY_API_SCOPE = "https://www.googleapis.com/auth/cloud-identity.policies.readonly";
    private static final String POLICY_API_BASE = "https://cloudidentity.googleapis.com/v1/policies";
    private static final long CACHE_TTL_MS = 15 * 60 * 1000L; // 15 min (Policy API: 1 QPS per customer)

    private final GoogleApiFactory directoryFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicReference<List<JsonNode>> cachedPolicies = new AtomicReference<>();
    private final AtomicLong policyCacheTimestamp = new AtomicLong(0);

    private final AtomicReference<Map<String, String>> cachedOuIdToPath = new AtomicReference<>();
    private final AtomicLong ouCacheTimestamp = new AtomicLong(0);

    private final Object policyLock = new Object();
    private final Object ouLock = new Object();

    public PolicyApiCacheService(GoogleApiFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    /**
     * Invalidates the policy cache so the next request fetches fresh data.
     */
    public void forceRefreshCache() {
        policyCacheTimestamp.set(0);
        ouCacheTimestamp.set(0);
    }

    public List<JsonNode> getAllPolicies(String adminEmail) throws Exception {
        long now = System.currentTimeMillis();
        List<JsonNode> currentPolicies = cachedPolicies.get();
        long currentTimestamp = policyCacheTimestamp.get();

        // 1st Check
        if (currentPolicies != null && (now - currentTimestamp) < CACHE_TTL_MS) {
            log.debug("Returning {} cached policies", currentPolicies.size());
            return currentPolicies;
        }

        // Cache miss: synchronize to ensure only one thread fetches
        synchronized (policyLock) {
            // Re-read inside the lock (another thread might have updated it)
            currentPolicies = cachedPolicies.get();
            currentTimestamp = policyCacheTimestamp.get();
            now = System.currentTimeMillis();

            // 2nd Check
            if (currentPolicies != null && (now - currentTimestamp) < CACHE_TTL_MS) {
                return currentPolicies;
            }

            // Fetch and update cache safely
            List<JsonNode> fresh = fetchAllPolicies(adminEmail);
            List<JsonNode> immutableFresh = List.copyOf(fresh); // Still use copyOf for immutability

            cachedPolicies.set(immutableFresh);
            policyCacheTimestamp.set(System.currentTimeMillis());

            return immutableFresh;
        }
    }

    /**
     * Returns a cached mapping from OU ID
     */
    public Map<String, String> getOuIdToPathMap(String adminEmail) throws Exception {
        long now = System.currentTimeMillis();
        Map<String, String> currentMap = cachedOuIdToPath.get();
        long currentTimestamp = ouCacheTimestamp.get();

        // 1st Check
        if (currentMap != null && (now - currentTimestamp) < CACHE_TTL_MS) {
            return currentMap;
        }

        synchronized (ouLock) {
            currentMap = cachedOuIdToPath.get();
            currentTimestamp = ouCacheTimestamp.get();
            now = System.currentTimeMillis();

            // 2nd Check
            if (currentMap != null && (now - currentTimestamp) < CACHE_TTL_MS) {
                return currentMap;
            }

            Map<String, String> fresh = fetchOuIdToPathMap(adminEmail);
            Map<String, String> immutableFresh = Map.copyOf(fresh);

            cachedOuIdToPath.set(immutableFresh);
            ouCacheTimestamp.set(System.currentTimeMillis());

            return immutableFresh;
        }
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
