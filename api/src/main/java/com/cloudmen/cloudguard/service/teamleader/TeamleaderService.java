package com.cloudmen.cloudguard.service.teamleader;

import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TeamleaderService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderService.class);

    private final SupabaseTokenService supabaseTokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${teamleader.client.id}") private String clientId;
    @Value("${teamleader.client.secret}") private String clientSecret;

    public TeamleaderService(SupabaseTokenService supabaseTokenService) {
        this.supabaseTokenService = supabaseTokenService;
    }

    public void updateCredentials(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AccessTokenEmptyException("Access token cannot be empty.");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenEmptyException("Refresh token cannot be empty.");
        }

        // Sla de tokens direct op in Supabase
        supabaseTokenService.updateTokens(accessToken, refreshToken);
        log.info("Teamleader credentials manually updated via setup endpoint (Saved to Supabase).");
    }


    public HttpHeaders createHeaders() {
        // Haal altijd de meest verse token op uit Supabase!
        SupabaseTokenService.TeamleaderTokens tokens = supabaseTokenService.getTokens();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    public boolean refreshTokens() {
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
