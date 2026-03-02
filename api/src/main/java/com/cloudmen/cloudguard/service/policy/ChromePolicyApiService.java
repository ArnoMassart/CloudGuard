package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service to fetch Chrome policies via Chrome Policy API (chromepolicy.googleapis.com).
 * Used for Chrome extension policies (ExtensionSettings, ExtensionInstallBlocklist, etc.).
 *
 * Requires: Chrome Policy API enabled in GCP, scope chrome.management.policy.readonly,
 * and domain-wide delegation for the service account with Chrome Management admin rights.
 */
@Service
public class ChromePolicyApiService {
    private static final Logger log = LoggerFactory.getLogger(ChromePolicyApiService.class);
    private static final String CHROME_POLICY_SCOPE = "https://www.googleapis.com/auth/chrome.management.policy.readonly";
    private static final String CHROME_POLICY_RESOLVE_URL = "https://chromepolicy.googleapis.com/v1/customers/my_customer/policies:resolve";

    private final GoogleApiFactory googleApiFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ChromePolicyApiService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    /**
     * Resolves Chrome user policies for the given org unit.
     * Fetches ExtensionSettings and legacy extension policies (ExtensionInstallBlocklist, etc.).
     *
     * @param adminEmail  Admin email for impersonation
     * @param orgUnitId   Org unit ID (from Directory API)
     * @return Map of policy schema name to resolved value, or empty map on failure
     */
    public Map<String, JsonNode> resolveChromePoliciesForOrgUnit(String adminEmail, String orgUnitId) throws Exception {
        if (orgUnitId == null || orgUnitId.isBlank()) {
            return Collections.emptyMap();
        }

        var creds = googleApiFactory.getCredentials(Set.of(CHROME_POLICY_SCOPE), adminEmail);
        creds.refreshIfExpired();
        String token = creds.getAccessToken().getTokenValue();

        Map<String, JsonNode> result = new HashMap<>();

        // Fetch ExtensionSettings and legacy extension policies (chrome.users.* covers ExtensionInstallBlocklist, etc.)
        for (String schemaFilter : List.of("chrome.users.appsconfig.*", "chrome.users.ExtensionInstall*")) {
            try {
                ObjectNode body = mapper.createObjectNode();
                body.put("policySchemaFilter", schemaFilter);
                ObjectNode targetKey = mapper.createObjectNode();
                targetKey.put("targetResource", "orgunits/" + orgUnitId);
                body.set("policyTargetKey", targetKey);
                body.put("pageSize", 100);

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<String> resp = restTemplate.exchange(
                        CHROME_POLICY_RESOLVE_URL,
                        HttpMethod.POST,
                        new HttpEntity<>(body.toString(), headers),
                        String.class);

                if (resp.getBody() != null) {
                    JsonNode root = mapper.readTree(resp.getBody());
                    JsonNode resolvedPolicies = root.get("resolvedPolicies");
                    if (resolvedPolicies != null && resolvedPolicies.isArray()) {
                        for (JsonNode rp : resolvedPolicies) {
                            String schema = rp.path("value").path("policySchema").asText("");
                            JsonNode value = rp.path("value").path("value");
                            if (!schema.isBlank() && !value.isMissingNode()) {
                                result.put(schema, value);
                            }
                        }
                    }
                }
            } catch (HttpStatusCodeException e) {
                log.warn("Chrome Policy API request failed for {}: {} - {}",
                        schemaFilter, e.getStatusCode(), e.getResponseBodyAsString());
                // Continue with other filters
            }
        }

        return result;
    }
}
