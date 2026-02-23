package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleDirectoryFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Component
public class ContextAwareAccessProvider implements OrgUnitPolicyProvider {
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String ACM_API_BASE = "https://accesscontextmanager.googleapis.com/v1";
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final GoogleDirectoryFactory directoryFactory;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${google.gcp.organization-id:}")
    private String orgId;

    public ContextAwareAccessProvider(GoogleDirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    @Override public String key() { return "caa"; }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        if (orgId == null || orgId.isBlank()) {
            return new OrgUnitPolicyDto(key(),
                    "Context-Aware Access",
                    "Toegangsbeleid op basis van context",
                    "Niet geconfigureerd",
                    "bg-slate-100 text-slate-700",
                    "Geen GCP organization-id ingesteld.",
                    false,
                    "Access Context Manager API",
                    SETTINGS_LINK_TEXT,
                    "accesscontextmanager");
        }

        GoogleCredentials creds = directoryFactory.getCredentials(Set.of(CLOUD_PLATFORM_SCOPE), adminEmail);
        creds.refreshIfExpired();
        String token = creds.getAccessToken().getTokenValue();

        String url = ACM_API_BASE + "/accessPolicies?parent=organizations/" + orgId.trim();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (resp.getBody() == null) {
            return new OrgUnitPolicyDto(key(),
                    "Context-Aware Access",
                    "Toegangsbeleid op basis van context",
                    "Kon niet ophalen",
                    "bg-amber-100 text-amber-800",
                    "Geen antwoord van Access Context Manager.",
                    false,
                    "Access Context Manager API",
                    SETTINGS_LINK_TEXT,
                    "accesscontextmanager");
        }

        JsonNode root = mapper.readTree(resp.getBody());
        JsonNode accessPolicies = root.get("accessPolicies");
        int count = (accessPolicies != null && accessPolicies.isArray()) ? accessPolicies.size() : 0;

        String status = (count > 0) ? "Actief (" + count + ")" : "Niet geconfigureerd";
        String css = (count > 0) ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-700";

        return new OrgUnitPolicyDto(key(),
                "Context-Aware Access",
                "Toegangsbeleid op basis van context",
                status,
                css,
                "Dit is org-level (niet OU-level) info uit Access Context Manager.",
                false,
                "Access Context Manager API",
                SETTINGS_LINK_TEXT,
                "accesscontextmanager");
    }
}
