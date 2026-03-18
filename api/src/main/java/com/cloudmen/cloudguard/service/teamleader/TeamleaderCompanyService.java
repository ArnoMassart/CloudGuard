package com.cloudmen.cloudguard.service.teamleader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@SuppressWarnings("unchecked")
public class TeamleaderCompanyService {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    public Map<String, Object> getCompanyDetails(String companyId, HttpHeaders headers) {
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

    public String getCompanyIdByEmail(String email, HttpHeaders headers) {
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

    public String getCompanyNameByEmail(String loggedInEmail, HttpHeaders headers) {
        String companyId = getCompanyIdByEmail(loggedInEmail, headers);

        if (companyId == null) {
            return "Onbekend Bedrijf";
        }

        Map<String, Object> companyDetails = getCompanyDetails(companyId, headers);

        if (companyDetails != null && companyDetails.containsKey("name")) {
            return (String) companyDetails.get("name");
        }

        return "Onbekend Bedrijf";
    }
}
