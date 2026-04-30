package com.cloudmen.cloudguard.service.teamleader;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.exception.TeamleaderResponseBodyEmptyException;
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

/**
 * A central service for managing Teamleader OAuth2 authentication and token lifecycle. <p>
 *
 * This service coordinates the storage and retrieval of access and refresh tokens, ensuring they are synchronized with
 * the Supabase backend. It provides utility methods to generate authenticated headers for API requests and handles
 * the "refresh_token" grant flow when existing tokens expire.
 */
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

    /**
     * Manually updates the Teamleader credentials and persists them to the Supabase database.
     *
     * @param accessToken   the new short-lived access token
     * @param refreshToken  the new long-lived refresh token
     * @throws AccessTokenEmptyException    if the access token is null or blank
     * @throws RefreshTokenEmptyException   if the refresh token is null or blank
     */
    public void updateCredentials(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AccessTokenEmptyException("Access token cannot be empty.");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenEmptyException("Refresh token cannot be empty.");
        }

        supabaseTokenService.updateTokens(accessToken, refreshToken);
        log.info("Teamleader credentials manually updated via setup endpoint (Saved to Supabase).");
    }


    /**
     * Generates a set of HTTP headers pre-configured with the latest Bearer authentication token from Supabase.
     *
     * @return a {@link HttpHeaders} containing the current Bearer access token
     */
    public HttpHeaders createHeaders() {
        TeamleaderTokens tokens = supabaseTokenService.getTokens();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    /**
     * Executes the OAuth2 refresh token flow to obtain a new set of credentials from Teamleader Focus. <p>
     *
     * Upon success, the new tokens are immediately persisted to Supabase to maintain service continuity.
     *
     * @return {@code true} if tokens were successfully refreshed; {@code false} otherwise
     * @throws TeamleaderResponseBodyEmptyException if the API returns a null body
     */
    public boolean refreshTokens() {
        TeamleaderTokens tokens = supabaseTokenService.getTokens();

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
            if (body == null) {
                throw new TeamleaderResponseBodyEmptyException("Response body van Teamleader was leeg tijdens het vernieuwen van tokens.");
            }

            String newAccessToken = (String) body.get("access_token");
            String newRefreshToken = (String) body.get("refresh_token");

            supabaseTokenService.updateTokens(newAccessToken, newRefreshToken);
            return true;

        } catch (Exception e) {
            log.error("Fout tijdens ophalen nieuwe Teamleader tokens: {}", e.getMessage());
            return false;
        }
    }
}
