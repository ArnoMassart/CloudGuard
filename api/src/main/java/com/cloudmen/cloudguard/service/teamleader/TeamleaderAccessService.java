package com.cloudmen.cloudguard.service.teamleader;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Service responsible for verifying if a customer has an active CloudGuard subscription based on Teamleader CRM
 * data. <p>
 *
 * This service acts as a gatekeeper, checking custom fields within the Teamleader API to determine access rights.
 * It includes robust error-handling logic, including automatic token refreshment and network-level retries.
 */
@Service
@SuppressWarnings("unchecked")
public class TeamleaderAccessService {
    private static final Logger log = LoggerFactory.getLogger(TeamleaderAccessService.class);

    private final TeamleaderService teamleaderService;
    private final TeamleaderCompanyService teamleaderCompanyService;
    private final UserService userService;
    private final OrganizationService organizationService;

    @Value("${teamleader.customfield.cloudguard.id}")
    private String cloudGuardFieldId;

    public TeamleaderAccessService(TeamleaderService teamleaderService, TeamleaderCompanyService teamleaderCompanyService,
            UserService userService, OrganizationService organizationService) {
        this.teamleaderService = teamleaderService;
        this.teamleaderCompanyService = teamleaderCompanyService;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    /**
     * Determines if the organization associated with the given email address has active access to CloudGuard. <p>
     *
     * <b>Resiliency Features:</b>
     * <ul>
     * <li><b>401 Handling:</b> Automatically attempts to refresh
     * OAuth tokens and retries once if unauthorized.</li>
     * <li><b>Network Retries:</b> Retries up to 2 times upon
     * encountering network-level exceptions (e.g., Connection Reset).</li>
     * </ul>
     *
     * @param loggedInEmail the email address of the user/admin being checked
     * @return {@code true} if the associated company has the CloudGuard custom field set to true in Teamleader;
     * {@code false} otherwise
     */
    public boolean hasCloudGuardAccess(String loggedInEmail) {
        int maxRetries = 2;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                log.info("Checking CloudGuard Access (Poging {})", attempt + 1);
                return executeAccessCheckFlow(loggedInEmail);

            } catch (HttpClientErrorException.Unauthorized e) {
                log.warn("Teamleader token verlopen (401) tijdens hasCloudGuardAccess, proberen te vernieuwen...");

                if (teamleaderService.refreshTokens()) {
                    log.info("Token succesvol vernieuwd, API call opnieuw uitvoeren.");
                    try {
                        return executeAccessCheckFlow(loggedInEmail);
                    } catch (Exception ex) {
                        log.error("API Call faalde definitief na token refresh: {}", ex.getMessage());
                        return false;
                    }
                }
                log.error("Vernieuwen van Teamleader token is mislukt.");
                return false;

            } catch (ResourceAccessException e) {
                // Dit vangt de 'Connection reset' en andere netwerk timeouts af
                attempt++;
                log.warn("Netwerkfout tijdens Teamleader/Supabase API Call (Connection reset). Poging {} van {}", attempt, maxRetries);

                if (attempt >= maxRetries) {
                    log.error("Definitieve netwerkfout na retries: {}", e.getMessage());
                    return false;
                }

                // Kleine pauze voordat we het opnieuw proberen (geeft de server ademruimte)
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

            } catch (Exception e) {
                log.error("Onverwachte fout tijdens Teamleader API Call: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Updates the Teamleader credentials stored in the underlying {@link TeamleaderService}.
     *
     * @param accessToken   the new OAuth access token
     * @param refreshToken  the new OAuth refresh token
     */
    public void updateCredentials(String accessToken, String refreshToken) {
        teamleaderService.updateCredentials(accessToken, refreshToken);
    }

    private boolean executeAccessCheckFlow(String loggedInEmail) {
        HttpHeaders headers = teamleaderService.createHeaders();

        Long organizationId = userService.findByEmailOptional(loggedInEmail).map(User::getOrganizationId).orElse(null);

        String companyId = null;
        if (organizationId != null) {
            Optional<Organization> orgOpt = organizationService.findById(organizationId);
            if (orgOpt.isPresent()) {
                String stored = orgOpt.get().getTeamleaderCompanyId();
                if (stored != null && !stored.isBlank()) {
                    companyId = stored;
                    log.debug("Teamleader access: using stored company id {} for organizationId={}", companyId, organizationId);
                }
            }
        }

        boolean resolvedViaStoredId = companyId != null;

        if (companyId == null) {
            companyId = teamleaderCompanyService.getCompanyIdByDomain(loggedInEmail, headers);
        }

        if (companyId == null) {
            log.info(
                    "Teamleader access: geen bedrijf gevonden (login-email={}; bedrijf wordt opgelost via domein uit dit adres)",
                    loggedInEmail);
            return false;
        }

        Map<String, Object> companyDetails = teamleaderCompanyService.getCompanyDetails(companyId, headers);
        if (companyDetails == null) {
            log.warn("Teamleader access: companies.info returned null for companyId={}", companyId);
            return false;
        }

        log.debug(
                "Teamleader access check: companyId={}, hasCustomFields={}",
                companyId,
                companyDetails.get("custom_fields") != null);

        boolean allowed = extractCloudGuardAccessValue(companyDetails);

        if (allowed && organizationId != null && !resolvedViaStoredId) {
            organizationService.saveTeamleaderCompanyIdIfAbsent(organizationId, companyId);
        }

        return allowed;
    }

    private boolean extractCloudGuardAccessValue(Map<String, Object> companyDetails) {
        List<Map<String, Object>> customFields = (List<Map<String, Object>>) companyDetails.get("custom_fields");

        if (customFields == null || customFields.isEmpty()) {
            log.warn(
                    "Teamleader access: company has no custom_fields (or empty). Cannot evaluate CloudGuard field id {}",
                    cloudGuardFieldId);
            return false;
        }

        for (Map<String, Object> field : customFields) {
            Map<String, Object> definition = (Map<String, Object>) field.get("definition");
            if (definition == null) {
                continue;
            }
            Object defId = definition.get("id");
            log.debug("Teamleader custom field candidate definition.id={}", defId);

            if (cloudGuardFieldId.equals(defId)) {
                Object value = field.get("value");
                log.info(
                        "Teamleader access: matched CloudGuard field id={}, value={}, valueType={}",
                        cloudGuardFieldId,
                        value,
                        value == null ? "null" : value.getClass().getSimpleName());

                boolean allowed = Boolean.TRUE.equals(value);
                if (!allowed) {
                    log.warn(
                            "Teamleader access: CloudGuard field is not boolean true (raw value={}). Access denied.",
                            value);
                }
                return allowed;
            }
        }
        log.warn(
                "Teamleader access: no custom field matched cloudGuardFieldId={}. Check application.properties vs CRM field definition id.",
                cloudGuardFieldId);

        return false;
    }
}
