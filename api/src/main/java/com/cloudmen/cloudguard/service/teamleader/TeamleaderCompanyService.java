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

/**
 * A service responsible for retrieving detailed company information from the Teamleader API. <p>
 *
 * This service handles low-level API interactions, including fetching company IDs by domain name and retrieving full
 * company profiles. It implements a standard retry pattern to refresh OAuth tokens automatically upon encountering
 * a 401 Unauthorized response.
 */
@Service
public class TeamleaderCompanyService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderCompanyService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final TeamleaderService teamleaderService;

    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    @Value("${teamleader.customfield.primary-domain.id}") private String primaryDomainFieldId;


    public TeamleaderCompanyService(TeamleaderService teamleaderService) {
        this.teamleaderService = teamleaderService;
    }

    /**
     * Retrieves the primary company ID associated with a specific email domain. <p>
     *
     * This method uses a "term" search in Teamleader, which matches the domain against company names, emails, and
     * websites. It includes an automatic token refresh attempt if the initial call is unauthorized.
     *
     * @param email     the email address to extract the domain from
     * @param headers   the HTTP headers containing the current access token
     * @return the Teamleader UUID for the company, or {@code null} if no match is found
     */
    public String getCompanyIdByDomain(String email, HttpHeaders headers) {
        String domain = extractDomain(email);

        if (domain.isEmpty()) {
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

    /**
     * Retrieves the full metadata of a company by its ID. <p>
     *
     * @param companyId the unique identifier of the company
     * in Teamleader
     * @param headers   the HTTP headers containing the access token
     * @return a Map containing the raw company data and custom fields
     */
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

    /**
     * Resolves the human-readable company name for a given email.
     *
     * @param userEmail     the email address of the user
     * @param headers       the HTTP headers for API authentication
     * @return the company name, or "Onbekend Bedrijf" if
     * resolution fails
     */
    public String getCompanyNameByEmail(String userEmail, HttpHeaders headers) {
        // AANGEPAST: Roept nu getCompanyIdByDomain aan
        String companyId = getCompanyIdByDomain(userEmail, headers);

        if (companyId == null) {
            return "Onbekend Bedrijf";
        }

        Map<String, Object> companyDetails = getCompanyDetails(companyId, headers);

        if (companyDetails != null && companyDetails.containsKey("name")) {
            return (String) companyDetails.get("name");
        }

        return "Onbekend Bedrijf";
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

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }

}