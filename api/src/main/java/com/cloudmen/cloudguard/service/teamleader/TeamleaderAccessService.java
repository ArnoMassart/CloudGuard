package com.cloudmen.cloudguard.service.teamleader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;


@Service
@SuppressWarnings("unchecked")
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

    private final TeamleaderService teamleaderService;
    private final TeamleaderCompanyService teamleaderCompanyService;

    @Value("${teamleader.customfield.cloudguard.id}") private String cloudGuardFieldId;

    public TeamleaderAccessService(TeamleaderService teamleaderService, TeamleaderCompanyService teamleaderCompanyService) {
        this.teamleaderService = teamleaderService;
        this.teamleaderCompanyService = teamleaderCompanyService;
    }

    public void updateCredentials(String accessToken, String refreshToken) {
        teamleaderService.updateCredentials(accessToken, refreshToken);
    }

        public boolean hasCloudGuardAccess(String loggedInEmail) {
        try {
            log.info("Checking CloudGuard Access");
            return executeAccessCheckFlow(loggedInEmail);
        }catch (HttpClientErrorException.Unauthorized e) {
        log.warn("Teamleader token verlopen (401) tijdens hasCloudGuardAccess, proberen te vernieuwen...");

        // Als het token verlopen is, probeer het te vernieuwen
        if (teamleaderService.refreshTokens()) {
            log.info("Token succesvol vernieuwd, API call opnieuw uitvoeren.");
            try {
                // Tweede poging met het nieuwe token
                return executeAccessCheckFlow(loggedInEmail);
            } catch (Exception ex) {
                log.error("API Call faalde definitief na token refresh: {}", ex.getMessage());
                return false;
            }
        }

        log.error("Vernieuwen van Teamleader token is mislukt.");
        return false;

    } catch (Exception e) {
        log.error("Onverwachte fout tijdens Teamleader API Call: {}", e.getMessage());
        return false; // Veilige fallback voor de Gatekeeper
    }

    }

    private boolean executeAccessCheckFlow(String domainEmail) {
        HttpHeaders headers = teamleaderService.createHeaders();

        String companyId = teamleaderCompanyService.getCompanyIdByEmail(domainEmail, headers);
        if (companyId == null) {
            log.info("Geen bedrijf gevonden voor domein: {}", domainEmail);
            return false;
        }

        Map<String, Object> companyDetails = teamleaderCompanyService.getCompanyDetails(companyId, headers);
        if (companyDetails == null) {
            return false;
        }

        return extractCloudGuardAccessValue(companyDetails);
    }

    private boolean extractCloudGuardAccessValue(Map<String, Object> companyDetails) {
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) companyDetails.get("custom_fields");

        if (customFields != null) {
            for (Map<String, Object> field : customFields) {
                Map<String, Object> definition = (Map<String, Object>) field.get("definition");

                if (definition != null && cloudGuardFieldId.equals(definition.get("id"))) {
                    Object value = field.get("value");
                    return Boolean.TRUE.equals(value);
                }
            }
        }

        return false;
    }
}
