package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.TeamleaderCredential;
import com.cloudmen.cloudguard.repository.TeamleaderCredentialRepository;
import org.hibernate.annotations.NotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


@Service
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

    private final TeamleaderCredentialRepository repository;
    private final RestTemplate restTemplate;

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;
    @Value("${teamleader.customfield.cloudguard.id}") private String cloudGuardFieldId;
    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    public TeamleaderAccessService(TeamleaderCredentialRepository repository) {
        this.repository = repository;
        this.restTemplate  = new RestTemplate();
    }

    public void updateCredentials(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null || accessToken.isBlank() || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Access token and Refresh token cannot be empty.");
        }

        TeamleaderCredential creds = repository.findById("SINGLETON")
                .orElse(new TeamleaderCredential());

        creds.setAccessToken(accessToken);
        creds.setRefreshToken(refreshToken);
        creds.setUpdatedAt(LocalDateTime.now());

        repository.save(creds);

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

        ResponseEntity<Map> response = restTemplate.postForEntity(
                teamleaderApiBase + "/companies.info",
                entity,
                Map.class
        );

        return (Map<String, Object>) response.getBody().get("data");    }

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


        ResponseEntity<Map> response = restTemplate.postForEntity(
                teamleaderApiBase+"/companies.list",
                entity,
                Map.class
        );

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        if (data != null && !data.isEmpty()) {
            return (String) data.get(0).get("id");
        }

        return null;
    }

    private HttpHeaders createHeaders() {
        TeamleaderCredential creds = getCredentials();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(creds.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private boolean refreshTokens() {
        TeamleaderCredential creds = getCredentials();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", creds.getRefreshToken());
        map.add("grant_type", "refresh_token");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://focus.teamleader.eu/oauth2/access_token",
                    new HttpEntity<>(map, new HttpHeaders()), Map.class);

            Map<String, Object> body = response.getBody();
            creds.setAccessToken((String) body.get("access_token"));
            creds.setRefreshToken((String) body.get("refresh_token"));
            creds.setUpdatedAt(LocalDateTime.now());

            repository.save(creds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TeamleaderCredential getCredentials() {
        return repository.findById("SINGLETON").orElseThrow(() -> new RuntimeException("Geen Teamleader tokens gevonden in DB. Voer de initiële setup uit."));
    }
}
