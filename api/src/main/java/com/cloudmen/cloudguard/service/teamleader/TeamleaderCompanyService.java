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
import java.util.Locale;
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
@SuppressWarnings("unchecked") // teamleader returns json deserialized as nested Map / List without generics.
public class TeamleaderCompanyService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderCompanyService.class);

    private static final int PRIMARY_DOMAIN_LOOKUP_CAP = 50; //inspect the first N list results to cap latency and API calls

    private final RestTemplate restTemplate = new RestTemplate();
    private final TeamleaderService teamleaderService;

    @Value("${teamleader.api.base}") private String teamleaderApiBase;

    //stores the UUID that Teamleader assigns to a specific custom field definition
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
            log.debug("Teamleader getCompanyIdByDomain: cannot extract domain from email (null or no '@')");
            return null;
        }

        log.debug(
                "Teamleader getCompanyIdByDomain: extractedDomain={}, normalized={}",
                domain,
                normalizeDomain(domain));

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
        String normalizedRequested = normalizeDomain(domain);

        // 1) Prefer the CRM "Primary domain" custom field as the authoritative key.
        if (primaryDomainFieldId != null && !primaryDomainFieldId.isBlank()) {
            String byCustomField = lookupByPrimaryDomainCustomField(normalizedRequested, headers);
            if (byCustomField != null) {
                log.info(
                        "Teamleader: resolved companyId={} via CRM primary-domain custom field (normalized={})",
                        byCustomField,
                        normalizedRequested);
                return byCustomField;
            }
            log.debug(
                    "Teamleader: no company found via CRM primary-domain custom field for normalized={}, falling back to term search",
                    normalizedRequested);
        } else {
            log.debug("Teamleader primary-domain field id not configured; using legacy term search only");
        }

        // 2) Fallback
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

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = response.getBody() != null ? (List<Map<String, Object>>) response.getBody().get("data") : null;
        if (data == null || data.isEmpty()) {
            log.debug("Teamleader companies.list: zero hits for filterTerm={}", domain);
            return null;
        }

        int scanLimit = Math.min(data.size(), PRIMARY_DOMAIN_LOOKUP_CAP);
        log.debug(
                "Teamleader companies.list: totalHits={}, scanLimit={}, filterTerm={}",
                data.size(),
                scanLimit,
                domain);

        //if primary domain is empty, behave like old behavior
        if(primaryDomainFieldId == null || primaryDomainFieldId.isBlank()){
            String fallbackId = (String) data.get(0).get("id");
            log.debug(
                    "Teamleader primary-domain field id not configured; using first companies.list hit companyId={}",
                    fallbackId);
            return fallbackId;
        }

        //if primary domain field is not empty,  normalize the domain (which comes from the email in getCompanyIdByDomain)
        boolean sawNonEmptyPrimaryDomain = false;

        log.debug(
                "Teamleader primary-domain scan: normalizedRequested={}, primaryDomainFieldId={}",
                normalizedRequested,
                primaryDomainFieldId);

        //loop over candidates in custom fields
        for(int i = 0; i< scanLimit; i++){
            String companyId = (String) data.get(i).get("id");
            if(companyId == null)
                continue;

            Map<String, Object> details = performGetCompanyDetails(companyId, headers);
            String crmPrimary = extractPrimaryDomainFromCompany(details);
            String companyName = details != null && details.get("name") != null
                    ? String.valueOf(details.get("name"))
                    : "";

            if(crmPrimary != null && !crmPrimary.isEmpty()){
                sawNonEmptyPrimaryDomain = true;
                String normalizedCrm = normalizeDomain(crmPrimary);
                boolean matches = normalizedCrm.equals(normalizedRequested);
                log.debug(
                        "Teamleader primary-domain scan [{}]: companyId={}, name={}, crmPrimaryRaw={}, normalizedCrm={}, matchesRequested={}",
                        i,
                        companyId,
                        companyName,
                        crmPrimary,
                        normalizedCrm,
                        matches);
                if(matches){
                    log.info(
                            "Teamleader: resolved companyId={} by CRM primary domain match (normalized={})",
                            companyId,
                            normalizedRequested);
                    return companyId;
                }
            } else {
                log.debug(
                        "Teamleader primary-domain scan [{}]: companyId={}, name={}, crmPrimaryDomain=<empty or absent>",
                        i,
                        companyId,
                        companyName);
            }
        }

        if (!sawNonEmptyPrimaryDomain) {
            log.warn(
                    "Teamleader: primary domain CRM field empty for first {} list hits; using first company.",
                    scanLimit);
            return (String) data.get(0).get("id");
        }
        log.warn(
                "Teamleader: no company CRM primary domain matches filterTerm={} (normalizedRequested={})",
                domain,
                normalizedRequested);
        return null;
    }

    private String lookupByPrimaryDomainCustomField(String normalizedDomain, HttpHeaders headers) {
        String body = """
        {
          "filter": {
            "custom_fields": [
              { "id": "%s", "value": "%s" }
            ]
          }
        }
        """.formatted(primaryDomainFieldId, normalizedDomain);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                teamleaderApiBase + "/companies.list",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = response.getBody() != null
                ? (List<Map<String, Object>>) response.getBody().get("data")
                : null;
        if (data == null || data.isEmpty()) {
            return null;
        }
        log.debug(
                "Teamleader custom-field filter: hits={}, normalizedDomain={}, fieldId={}",
                data.size(), normalizedDomain, primaryDomainFieldId);
        int scanLimit = Math.min(data.size(), PRIMARY_DOMAIN_LOOKUP_CAP);
        for (int i = 0; i < scanLimit; i++) {
            String companyId = (String) data.get(i).get("id");
            if (companyId == null) continue;
            Map<String, Object> details = performGetCompanyDetails(companyId, headers);
            String crmPrimary = extractPrimaryDomainFromCompany(details);
            if (crmPrimary != null && normalizeDomain(crmPrimary).equals(normalizedDomain)) {
                return companyId;
            }
        }
        return null;
    }

    private String extractPrimaryDomainFromCompany(Map<String, Object> companyDetails) {
        if(companyDetails==null){
            return null;
        }
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) companyDetails.get("custom_fields");
        if(customFields==null || customFields.isEmpty()){
            return null;
        }

        for(Map<String, Object> field: customFields){
            Map<String, Object> definition = (Map<String,Object>) field.get("definition");
            if(definition==null){
                continue;
            }
            if(!primaryDomainFieldId.equals(String.valueOf(definition.get("id")))){
                continue;
            }
            return stringifyCustomFieldValue(field.get("value"));
        }
        return null;
    }

    private String stringifyCustomFieldValue(Object value){
        if(value==null){
            return null;
        }
        if (value instanceof String s) {
            s = s.trim();
            return s.isEmpty() ? null : s;
        }
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text == null) {
                text = map.get("value");
            }
            if (text == null) {
                return null;
            }
            String t = text.toString().trim();
            return t.isEmpty() ? null : t;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    static String normalizeDomain(String raw){
        if(raw==null){
            return "";
        }

        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("https://")) {
            s = s.substring(8);
        } else if (s.startsWith("http://")) {
            s = s.substring(7);
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        if (s.startsWith("www.")) {
            s = s.substring(4);
        }
        return s;
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