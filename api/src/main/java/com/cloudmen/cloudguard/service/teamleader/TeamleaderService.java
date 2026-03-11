package com.cloudmen.cloudguard.service.teamleader;

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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TeamleaderService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderService.class);

    private final TeamleaderCredentialRepository repository;
    private final RestTemplate restTemplate;

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;

    private static final String REFRESH_TOKEN_TEXT = "refresh_token";

    public TeamleaderService(TeamleaderCredentialRepository repository) {
        this.repository = repository;
        this.restTemplate = new RestTemplate();
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

    public HttpHeaders createHeaders() {
        TeamleaderCredential credentials = getCredentials();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(credentials.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    public boolean refreshTokens() {
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
        return repository.findById("SINGLETON").orElseThrow(() -> new IllegalArgumentException("Geen Teamleader tokens gevonden in DB. Voer de initiële setup uit."));
    }
}
