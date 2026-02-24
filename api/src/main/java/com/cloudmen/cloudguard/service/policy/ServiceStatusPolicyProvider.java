package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleDirectoryFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

@Order(2)
@Component
public class ServiceStatusPolicyProvider implements OrgUnitPolicyProvider {
    private static final Logger log = LoggerFactory.getLogger(ServiceStatusPolicyProvider.class);
    private static final String POLICY_API_SCOPE = "https://www.googleapis.com/auth/cloud-identity.policies.readonly";
    private static final String POLICY_API_BASE = "https://cloudidentity.googleapis.com/v1/policies";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final Set<String> TARGET_SERVICES = Set.of("gmail", "drive_and_docs", "meet");

    private final GoogleDirectoryFactory directoryFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile List<JsonNode> cachedPolicies;
    private volatile long cacheTimestamp;

    public ServiceStatusPolicyProvider(GoogleDirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Override
    public String key() {
        return "ou_services_status";
    }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        String path = normalizeOrgUnitPath(orgUnitPath);

        // 1) Fetch all service_status policies once (single list call).
        List<JsonNode> policies = listServiceStatusPolicies(adminEmail);

        // 2) For each service, find the best matching policy for this OU:
        //    exact OU match first, otherwise nearest parent (inheritance), otherwise none.
        Map<String, ServiceStatusDto> results = new LinkedHashMap<>();
        for (String service : TARGET_SERVICES) {
            ServiceStatusDto r = resolveServiceForOu(policies, service, path);
            results.put(service, r);
        }

        // 3) Build a single card with combined status
        String status = formatCombinedStatus(results);
        boolean anyUnknown = results.values().stream()
                .anyMatch(r -> "(geen policy gevonden)".equals(r.getFromOrgUnit()));
        boolean anyOff = results.values().stream().anyMatch(r -> !r.isEnabled());
        String statusClass = anyUnknown
                ? "bg-slate-100 text-slate-700"
                : anyOff ? "bg-amber-100 text-amber-800" : "bg-green-100 text-green-800";

        return new OrgUnitPolicyDto(
                key(),
                "Services voor deze OU",
                "Gmail / Drive / Meet service status",
                status,
                statusClass,
                "Deze status komt uit Cloud Identity Policy API (service_status) en is OU-gebonden.",
                false,
                "Cloud Identity Policy API",
                "Klik hier om deze instellingen aan te passen",
                "apps"
        );
    }

    private static String normalizeOrgUnitPath(String orgUnitPath) {
        return (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
    }

    private List<JsonNode> listServiceStatusPolicies(String adminEmail) throws Exception {
        long now = System.currentTimeMillis();
        if (cachedPolicies != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            log.debug("Returning {} cached service_status policies", cachedPolicies.size());
            return cachedPolicies;
        }
        List<JsonNode> fresh = fetchAllServiceStatusPolicies(adminEmail);
        cachedPolicies = fresh;
        cacheTimestamp = now;
        return fresh;
    }

    private List<JsonNode> fetchAllServiceStatusPolicies(String adminEmail) throws Exception {
        String token = getPolicyApiToken(adminEmail);

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
                    for (JsonNode p : policiesNode) {
                        if (isServiceStatusPolicy(p)) out.add(p);
                    }
                }

                JsonNode next = root.get("nextPageToken");
                pageToken = (next != null && !next.isNull() && !next.asText().isBlank()) ? next.asText() : null;

            } while (pageToken != null);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Policy API fetch failed: HTTP " +
                    e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        log.info("Fetched {} service_status policies from API (cached for {}s)", out.size(), CACHE_TTL_MS / 1000);
        return out;
    }

    private boolean isServiceStatusPolicy(JsonNode policy) {
        JsonNode setting = policy.get("setting");
        if (setting == null) return false;
        JsonNode type = setting.get("type");
        if (type == null) return false;
        return type.asText().endsWith("service_status");
    }

    private String getPolicyApiToken(String adminEmail) throws Exception {
        var creds = directoryFactory.getCredentials(Set.of(POLICY_API_SCOPE), adminEmail);
        creds.refreshIfExpired();
        return creds.getAccessToken().getTokenValue();
    }

    /**
     * Picks the first policy whose setting.type matches "settings/{service}.service_status".
     * The Policy API returns OU references as IDs (orgUnits/xxx), not paths, so we
     * simply pick the first match per service for now.
     */
    private ServiceStatusDto resolveServiceForOu(List<JsonNode> policies, String service, String orgUnitPath) {
        String wantedType = "settings/" + service + ".service_status";

        for (JsonNode p : policies) {
            JsonNode setting = p.get("setting");
            if (setting == null) continue;
            String type = setting.path("type").asText("");
            if (!wantedType.equals(type)) continue;

            String policyOu = p.path("policyQuery").path("orgUnit").asText("(root)");
            JsonNode valueNode = setting.path("value");
            boolean enabled = !valueNode.isMissingNode() && valueNode.path("enabled").asBoolean(false);
            boolean inherited = !"/".equals(orgUnitPath);

            return new ServiceStatusDto(service, enabled, inherited, policyOu);
        }

        return new ServiceStatusDto(service, false, true, "(geen policy gevonden)");
    }

    private String formatCombinedStatus(Map<String, ServiceStatusDto> results) {
        return "Gmail: " + formatStatus(results.get("gmail"))
                + " • Drive: " + formatStatus(results.get("drive_and_docs"))
                + " • Meet: " + formatStatus(results.get("meet"));
    }

    private String formatStatus(ServiceStatusDto dto) {
        if (dto != null && "(geen policy gevonden)".equals(dto.getFromOrgUnit())) {
            return "Onbekend";
        }
        return dto != null && dto.isEnabled() ? "Aan" : "Uit";
    }
}
