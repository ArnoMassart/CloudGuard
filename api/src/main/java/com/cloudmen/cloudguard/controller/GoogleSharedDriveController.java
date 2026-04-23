package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.service.GoogleSharedDriveService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for managing Google Workspace Shared Drives. <p>
 *
 * This controller provides endpoints to fetch paginated lists of shared drives, search for specific drives,
 * retrieve high-level overviews including security metrics, and manually trigger cache refreshes. <p>
 *
 * All routes are mapped under the {@code /google/drives} prefix.
 */
@RestController
@RequestMapping("/google/drives")
public class GoogleSharedDriveController {
    private final GoogleSharedDriveService driveService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * Constructs a new {@link GoogleSharedDriveController} with the required services.
     *
     * @param driveService      the service handling Google Shared Drive data
     * @param jwtService        the service used to validate the session token
     * @param preferenceService the service managing user security preferences
     */
    public GoogleSharedDriveController(GoogleSharedDriveService driveService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.driveService = driveService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Retrieves a paginated and conditionally filtered list of shared drives. <p>
     *
     * This endpoint allows clients to browse through the organization's shared drives. It supports pagination via a
     * page token and a customizable page size, as well as filtering via an optional search query.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param pageToken the token indicating which page of results to fetch
     * @param size      the maximum number of drives to return (default is 2)
     * @param query     an optional search term to filter drives by name
     * @return a {@link ResponseEntity} containing a {@link SharedDrivePageResponse} with the requested drives and
     * pagination details
     */
    @GetMapping
    public ResponseEntity<SharedDrivePageResponse> getDrives(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(required = false) String query) {

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(driveService.getSharedDrivesPaged(loggedInEmail, pageToken, size, query));
    }

    /**
     * Retrieves a high-level overview of the organization's shared drives. <p>
     *
     * This includes aggregated metrics such as the total number of drives, external sharing configurations, and
     * potential security risks. The response is tailored based on the authenticated user's disabled security
     * preferences.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link SharedDriveOverviewResponse} with aggregated shared
     * drive metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<SharedDriveOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(driveService.getDrivesPageOverview(adminEmail, preferenceService.getDisabledPreferenceKeys(adminEmail)));
    }

    /**
     * Forces a manual refresh of the shared drives cache. <p>
     *
     * This endpoint is useful when a user want to immediately synchronize the local system with the latest
     * shared drives data from Google Workspace, bypassing the scheduled caching intervals.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} confirming that the cache was successfully refreshed
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String adminEmail = jwtService.validateInternalToken(token);
        driveService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
