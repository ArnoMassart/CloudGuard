package com.cloudmen.cloudguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class SupabaseTokenService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseTokenService.class);

    private final RestTemplate restTemplate;
    private final String supabaseUrl;
    private final String supabaseKey;

    public record TeamleaderTokens(String accessToken, String refreshToken) {}

    public SupabaseTokenService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.key}") String supabaseKey) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(new JdkClientHttpRequestFactory());
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
    }

    private HttpHeaders getSupabaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        return headers;
    }

    public TeamleaderTokens getTokens() {
        String url = supabaseUrl + "/rest/v1/teamleader_tokens?id=eq.1";
        HttpEntity<Void> entity = new HttpEntity<>(getSupabaseHeaders());

        ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, String>> body = response.getBody();
        if (body != null && !body.isEmpty()) {
            Map<String, String> row = body.get(0);
            return new TeamleaderTokens(row.get("access_token"), row.get("refresh_token"));
        }
        throw new RuntimeException("Geen Teamleader tokens gevonden in Supabase! Voer de initiële setup uit.");
    }

    public void updateTokens(String accessToken, String refreshToken) {
        String url = supabaseUrl + "/rest/v1/teamleader_tokens?id=eq.1";

        Map<String, String> body = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, getSupabaseHeaders());

        restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
        log.info("Teamleader tokens succesvol gesynchroniseerd met centrale Supabase database.");
    }
}
