package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.licenses.LicenseOverviewResponse;
import com.cloudmen.cloudguard.dto.licenses.LicensePageResponse;
import com.cloudmen.cloudguard.service.GoogleLicenseService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for managing Google Workspace licenses. <p>
 *
 * This controller provides endpoints to fetch detailed license allocations, usage data, and high-level summaries
 * for the organization. <p>
 *
 * All routes are mapped under the {@code /google/license} prefix.
 */
@RestController
@RequestMapping("/google/license")
public class GoogleLicenseController {
    private final GoogleLicenseService licenseService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link GoogleLicenseController} with the required services.
     *
     * @param licenseService    the service handling Google Workspace license data
     * @param jwtService        the service used to validate the session token and extract the user's email address
     */
    public GoogleLicenseController(GoogleLicenseService licenseService, JwtService jwtService) {
        this.licenseService = licenseService;
        this.jwtService = jwtService;
    }

    /**
     * Retrieves detailed information about the organization's licenses. <p>
     *
     * This endpoint authenticates the user via the provided session cookie and fetches a comprehensive list or page
     * of current license assignments, availability, and usage details.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link LicensePageResponse} with the detailed license data
     */
    @GetMapping
    public ResponseEntity<LicensePageResponse> getLicenses(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(licenseService.getLicenses(adminEmail));
    }

    /**
     * Retrieves a high-level overview of the organization's license usage. <p>
     *
     * Similar to the detailed endpoint, this route validates the user's session and then fetches aggregated
     * statistics, such as total allocated licenses versus total available licenses.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link LicenseOverviewResponse} with high-level license metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<LicenseOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(licenseService.getLicensesPageOverview(loggedInEmail));
    }
}
