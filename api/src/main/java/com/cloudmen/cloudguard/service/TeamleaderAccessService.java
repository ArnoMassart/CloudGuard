package com.cloudmen.cloudguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
@SuppressWarnings("unchecked")
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

private final SupabaseTokenService supabaseTokenService;
private final RestTemplate restTemplate = new RestTemplate();

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;
    @Value("${teamleader.customfield.cloudguard.id}") private String cloudGuardFieldId;
    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    public TeamleaderAccessService(SupabaseTokenService supabaseTokenService) {
        this.supabaseTokenService = supabaseTokenService;
    }

    public void updateCredentials(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null || accessToken.isBlank() || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Access token and Refresh token cannot be empty.");
        }

        // Sla de tokens direct op in Supabase
        supabaseTokenService.updateTokens(accessToken, refreshToken);
        log.info("Teamleader credentials manually updated via setup endpoint (Saved to Supabase).");
    }

    public boolean hasCloudGuardAccess(String loggedInEmail) {
        try {
            log.info("Checking CloudGuard Access");
            return executeAccessCheckFlow(loggedInEmail);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Teamleader token verlopen (401), proberen te vernieuwen via centrale Supabase...");

            if (refreshTokens()) {
                log.info("Token succesvol vernieuwd, API call opnieuw uitvoeren.");
                try {
                    return executeAccessCheckFlow(loggedInEmail);
                } catch (Exception ex) {
                    log.error("API Call faalde definitief na token refresh: {}", ex.getMessage());
                    return false;
                }
            }

            log.error("Vernieuwen van Teamleader token is mislukt.");
            return false;

        } catch (Exception e) {
            log.error("Onverwachte fout tijdens Teamleader API Call: {}", e.getMessage());
            return false;
        }
    }

    private boolean executeAccessCheckFlow(String domainEmail) {
        HttpHeaders headers = createHeaders();

        String companyId = getCompanyIdByEmail(domainEmail, headers);
        if (companyId == null) {
            log.info("Geen bedrijf gevonden voor domein: {}", domainEmail);
            return false;
        }

        Map<String, Object> companyDetails = getCompanyDetails(companyId, headers);
        if (companyDetails == null) {
            return false;
        }

        return extractCloudGuardAccessValue(companyDetails);
    }

    private boolean extractCloudGuardAccessValue(Map<String, Object> companyDetails) {
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) companyDetails.get("custom_fields");

        if (customFields != null) {
            for (Map<String, Object> field : customFields) {
                Map<String, Object> definition = (Map<String, Object>) field.get("definition");

                if (definition != null && cloudGuardFieldId.equals(definition.get("id"))) {
                    Object value = field.get("value");
                    return Boolean.TRUE.equals(value);
                }
            }
        }
        return false;
    }

    private Map<String, Object> getCompanyDetails(String companyId, HttpHeaders headers) {
        String infoBody = "{\"id\": \"" + companyId + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(infoBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                teamleaderApiBase + "/companies.info",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("data");
    }

    private String getCompanyIdByEmail(String email, HttpHeaders headers) {
        String body = """
        {
          "filter": {
            "email": {
              "type": "primary",
              "email": "%s"
            }
          }
        }
        """.formatted(email);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                teamleaderApiBase + "/companies.list",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> data = (List<Map<String, Object>>) Objects.requireNonNull(response.getBody()).get("data");
        if (data != null && !data.isEmpty()) {
            return (String) data.get(0).get("id");
        }

        return null;
    }

    private HttpHeaders createHeaders() {
        // Haal altijd de meest verse token op uit Supabase!
        SupabaseTokenService.TeamleaderTokens tokens = supabaseTokenService.getTokens();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private boolean refreshTokens() {
        // Haal de refresh token centraal op
        SupabaseTokenService.TeamleaderTokens tokens = supabaseTokenService.getTokens();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", tokens.refreshToken());
        map.add("grant_type", "refresh_token");

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://focus.teamleader.eu/oauth2/access_token",
                    HttpMethod.POST,
                    new HttpEntity<>(map, new HttpHeaders()),
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            assert body != null;

            String newAccessToken = (String) body.get("access_token");
            String newRefreshToken = (String) body.get("refresh_token");

            // Schiet de nieuwe tokens direct door naar Supabase voor je collega
            supabaseTokenService.updateTokens(newAccessToken, newRefreshToken);
            return true;

        } catch (Exception e) {
            log.error("Fout tijdens ophalen nieuwe Teamleader tokens: {}", e.getMessage());
            return false;
        }
    }
}
