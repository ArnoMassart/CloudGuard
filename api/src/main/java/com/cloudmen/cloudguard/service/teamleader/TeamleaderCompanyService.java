package com.cloudmen.cloudguard.service.teamleader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@SuppressWarnings("unchecked")
public class TeamleaderCompanyService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderCompanyService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final TeamleaderService teamleaderService;

    @Value("${teamleader.api.base}") private String teamleaderApiBase;
    @Value("${teamleader.customfield.primary-domain.id}") private String primaryDomainFieldId;


    public TeamleaderCompanyService(TeamleaderService teamleaderService) {
        this.teamleaderService = teamleaderService;
    }

    public Map<String, Object> getCompanyDetails(String companyId, HttpHeaders headers) {
        try {
            return performGetCompanyDetails(companyId, headers);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Teamleader token verlopen (401) tijdens getCompanyDetails, proberen te vernieuwen...");
            if (teamleaderService.refreshTokens()) {
                return performGetCompanyDetails(companyId, teamleaderService.createHeaders());
            }
            throw e;
        }
    }

    private Map<String, Object> performGetCompanyDetails(String companyId, HttpHeaders headers) {
        String infoBody = "{\"id\": \"" + companyId + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(infoBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                teamleaderApiBase + "/companies.info",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        return (Map<String, Object>) Objects.requireNonNull(response.getBody()).get("data");
    }

    public String getCompanyIdByDomain(String email, HttpHeaders headers) {
        String domain = extractDomain(email);

        if (domain == null || domain.isEmpty()) {
            return null;
        }

        try {
            return performGetCompanyIdByDomain(domain, headers);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Teamleader token verlopen (401) tijdens getCompanyIdByDomain, proberen te vernieuwen...");
            if (teamleaderService.refreshTokens()) {
                return performGetCompanyIdByDomain(domain, teamleaderService.createHeaders());
            }
            throw e;
        }
    }

    private String performGetCompanyIdByDomain(String domain, HttpHeaders headers) {
        // Door 'term' te gebruiken, zoekt Teamleader breed naar het domein,
        // inclusief in de e-mailadressen van het bedrijf.
        String body = """
        {
          "filter": {
            "term": "%s"
          }
        }
        """.formatted(domain);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                teamleaderApiBase + "/companies.list",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        List<Map<String, Object>> data = (List<Map<String, Object>>) Objects.requireNonNull(response.getBody()).get("data");
        if (data != null && !data.isEmpty()) {
            // We pakken hier het eerste resultaat dat Teamleader teruggeeft.
            return (String) data.get(0).get("id");
        }

        return null;
    }

    // ==============================================================================
    // VERGEET NIET OM DEZE METHODES AAN TE PASSEN ZODAT ZE DE NIEUWE NAAM GEBRUIKEN:
    // ==============================================================================

    public String getCompanyNameByEmail(String loggedInEmail, HttpHeaders headers) {
        // AANGEPAST: Roept nu getCompanyIdByDomain aan
        String companyId = getCompanyIdByDomain(loggedInEmail, headers);

        if (companyId == null) {
            return "Onbekend Bedrijf";
        }

        Map<String, Object> companyDetails = getCompanyDetails(companyId, headers);

        if (companyDetails != null && companyDetails.containsKey("name")) {
            return (String) companyDetails.get("name");
        }

        return "Onbekend Bedrijf";
    }

    public boolean verifyUserDomainWithCompany(String loggedInEmail, HttpHeaders headers) {
        // AANGEPAST: Roept nu getCompanyIdByDomain aan
        String companyId = getCompanyIdByDomain(loggedInEmail, headers);

        if (companyId == null) {
            log.warn("Bedrijf niet gevonden op basis van domein voor email: {}", loggedInEmail);
            return false;
        }

        // ... de rest van je verifyUserDomainWithCompany methode blijft hetzelfde ...
        Map<String, Object> companyDetails = getCompanyDetails(companyId, headers);
        if (companyDetails == null) {
            return false;
        }

        String userDomain = extractDomain(loggedInEmail);

        // 1. Check het Custom Field "Primair Domein"
        String customFieldDomain = extractCustomFieldValue(companyDetails, primaryDomainFieldId);
        if (customFieldDomain != null && !customFieldDomain.isBlank()) {
            log.debug("Domein gevonden in custom field: {}", customFieldDomain);
            return userDomain.equalsIgnoreCase(customFieldDomain.trim());
        }

        // 2. Fallback: Check het primaire e-mailadres van het bedrijf
        String companyEmail = extractPrimaryEmail(companyDetails);
        if (companyEmail != null && !companyEmail.isBlank()) {
            String companyDomain = extractDomain(companyEmail);
            log.debug("Geen custom field, controleer bedrijfs-email domein: {}", companyDomain);
            return userDomain.equalsIgnoreCase(companyDomain);
        }

        log.warn("Geen Primair Domein custom field of bedrijfs-email gevonden voor bedrijf ID: {}", companyId);
        return false;
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }

    private String extractCustomFieldValue(Map<String, Object> companyDetails, String targetCustomFieldId) {
        if (!companyDetails.containsKey("custom_fields")) {
            return null;
        }

        List<Map<String, Object>> customFields = (List<Map<String, Object>>) companyDetails.get("custom_fields");
        for (Map<String, Object> field : customFields) {
            if (targetCustomFieldId.equals(field.get("id"))) {
                Object value = field.get("value");
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    private String extractPrimaryEmail(Map<String, Object> companyDetails) {
        if (!companyDetails.containsKey("emails")) {
            return null;
        }

        List<Map<String, Object>> emails = (List<Map<String, Object>>) companyDetails.get("emails");
        for (Map<String, Object> emailObj : emails) {
            if ("primary".equals(emailObj.get("type"))) {
                return (String) emailObj.get("email");
            }
        }
        return null;
    }
}