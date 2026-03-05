package com.cloudmen.cloudguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
public class TeamLeaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamLeaderAccessService.class);
    private final RestTemplate restTemplate;

    @Value("${teamleader.api.token}")
    private String teamleaderToken;

    public TeamLeaderAccessService() {
        this.restTemplate = new RestTemplate();
    }

    // Voeg dit toe aan TeamleaderAccessService.java
    public Map<String, Object> getRawCompanyData(String userEmail) {
        if (userEmail == null || !userEmail.contains("@")) return Map.of("error", "Ongeldig e-mailadres");
        String domain = userEmail.substring(userEmail.indexOf("@") + 1);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(teamleaderToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = """
            {
                "filter": { "email": { "type": "substring", "match": "%s" } },
                "page": { "size": 1 }
            }
            """.formatted(domain);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.focus.teamleader.eu/companies.list",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            return Map.of("error", "API Call faalde: " + e.getMessage());
        }
    }
}
