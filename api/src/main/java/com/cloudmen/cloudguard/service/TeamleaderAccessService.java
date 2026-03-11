package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.TeamleaderCredential;
import com.cloudmen.cloudguard.repository.TeamleaderCredentialRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
@SuppressWarnings("unchecked")
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

    private final TeamleaderCredentialRepository repository;
    private final RestTemplate restTemplate;

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;
    @Value("${teamleader.customfield.cloudguard.id}") private String cloudGuardFieldId;
    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    private static final String REFRESH_TOKEN_TEXT = "refresh_token";

    public TeamleaderAccessService(TeamleaderCredentialRepository repository) {
        this.repository = repository;
        this.restTemplate  = new RestTemplate();
    }

    public void updateCredentials(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null || accessToken.isBlank() || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Access token and Refresh token cannot be empty.");
        }

        TeamleaderCredential credentials = repository.findById("SINGLETON")
                .orElse(new TeamleaderCredential());

        credentials.setAccessToken(accessToken);
        credentials.setRefreshToken(refreshToken);
        credentials.setUpdatedAt(LocalDateTime.now());

        repository.save(credentials);

        log.info("Teamleader credentials manually updated via setup endpoint.");
    }

    public boolean hasCloudGuardAccess(String loggedInEmail) {
        try {
            log.info("Checking CloudGuard Access");
            return executeAccessCheckFlow(loggedInEmail);
        }catch (HttpClientErrorException.Unauthorized e) {
        log.warn("Teamleader token verlopen (401), proberen te vernieuwen...");

        // Als het token verlopen is, probeer het te vernieuwen
        if (refreshTokens()) {
            log.info("Token succesvol vernieuwd, API call opnieuw uitvoeren.");
            try {
                // Tweede poging met het nieuwe token
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
        return false; // Veilige fallback voor de Gatekeeper
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
                new ParameterizedTypeReference<>() {
                }
        );

        return (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("data");    }

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
                new ParameterizedTypeReference<>() {
                }
        );

        List<Map<String, Object>> data = (List<Map<String, Object>>) Objects.requireNonNull(response.getBody()).get("data");
        if (data != null && !data.isEmpty()) {
            return (String) data.get(0).get("id");
        }

        return null;
    }

    private HttpHeaders createHeaders() {
        TeamleaderCredential credentials = getCredentials();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private boolean refreshTokens() {
        TeamleaderCredential credentials = getCredentials();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add(REFRESH_TOKEN_TEXT, credentials.getRefreshToken());
        map.add("grant_type", REFRESH_TOKEN_TEXT);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://focus.teamleader.eu/oauth2/access_token",
                    HttpMethod.POST,
                    new HttpEntity<>(map, new HttpHeaders()), new ParameterizedTypeReference<>(){});

            Map<String, Object> body = response.getBody();
            assert body != null;
            credentials.setAccessToken((String) body.get("access_token"));
            credentials.setRefreshToken((String) body.get(REFRESH_TOKEN_TEXT));
            credentials.setUpdatedAt(LocalDateTime.now());

            repository.save(credentials);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TeamleaderCredential getCredentials() {
        return repository.findById("SINGLETON").orElseThrow(() -> new RuntimeException("Geen Teamleader tokens gevonden in DB. Voer de initiële setup uit."));
    }
}
