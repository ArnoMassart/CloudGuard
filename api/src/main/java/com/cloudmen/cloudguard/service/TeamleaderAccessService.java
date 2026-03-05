package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.TeamleaderCredential;
import com.cloudmen.cloudguard.repository.TeamleaderCredentialRepository;
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


@Service
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

    private final TeamleaderCredentialRepository repository;
    private final RestTemplate restTemplate;

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;
    @Value("${teamleader.customfield.cloudguard.id}") private String cloudGuardFieldId;

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
            Map<String, Object> rawData = getRawCompanyData(loggedInEmail);
            return parseAccessFromResponse(rawData);
        } catch (Exception e) {
            log.error("Gatekeeper check faalde {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getRawCompanyData(String userEmail) {
        if (userEmail == null || !userEmail.contains("@")) return Map.of("error", "Ongeldig e-mailadres");
        String domain = userEmail.substring(userEmail.indexOf("@") + 1);

        try {
            return callTeamleaderApi(domain);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Token verlopen, proberen te vernieuwen...");
            if (refreshTokens()) {
                return callTeamleaderApi(domain);
            }
            return Map.of("error", "Vernieuwen van token mislukt.");
        } catch (Exception e) {
            return Map.of("error", "API Call faalde: " + e.getMessage());
        }
    }

    private Map<String, Object> callTeamleaderApi(String domain) {
        TeamleaderCredential creds = getCredentials();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(creds.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"filter\": {\"email\": {\"type\": \"substring\", \"match\": \"" + domain + "\"}}, \"page\": {\"size\": 1}}";
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.focus.teamleader.eu/companies.list",
                new HttpEntity<>(body, headers),
                Map.class
        );
        return response.getBody();
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

    private boolean parseAccessFromResponse(Map<String, Object> body) {
        if (body == null || !body.containsKey("data")) return false;
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        if (data == null || data.isEmpty()) return false;

        List<Map<String, Object>> customFields = (List<Map<String, Object>>) data.get(0).get("custom_fields");
        if (customFields == null) return false;

        return customFields.stream()
                .filter(f -> cloudGuardFieldId.equals(f.get("id")))
                .map(f -> {
                    Object val = f.get("value");
                    return Boolean.TRUE.equals(val) || "true".equalsIgnoreCase(String.valueOf(val));
                })
                .findFirst()
                .orElse(false);
    }

    private TeamleaderCredential getCredentials() {
        return repository.findById("SINGLETON").orElseThrow(() -> new RuntimeException("Geen Teamleader tokens gevonden in DB. Voer de initiële setup uit."));
    }
}
