package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.dto.users.UsersWithoutTwoFactorResponse;
import com.cloudmen.cloudguard.service.GoogleUsersService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for managing Google Workspace users. <p>
 *
 * This controller provides endpoints to fetch paginated lists of users within the organization, identify users
 * lacking Two-Factor Authentication (2FA), retrieve high-level user metrics, and manually trigger cache refreshes. <p>
 *
 * All routes are mapped under the {@code /google/users} prefix.
 */
@RestController
@RequestMapping("/google/users")
public class GoogleUsersController {
    private final JwtService jwtService;
    private final GoogleUsersService googleUserService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * Constructs a new {@link GoogleUsersController} with the required services.
     *
     * @param jwtService        the service used to validate the session token
     * @param googleUserService the service handling Google Workspace user data
     * @param preferenceService the service managing user security preferences
     */
    public GoogleUsersController(JwtService jwtService, GoogleUsersService googleUserService, UserSecurityPreferenceService preferenceService) {
        this.jwtService = jwtService;
        this.googleUserService = googleUserService;
        this.preferenceService = preferenceService;
    }

    /**
     * Retrieves a paginated and conditionally filtered list of users within the organization. <p>
     *
     * This endpoint supports pagination via a page token and a customizable page size. It also allows searching for
     * specific users using an optional query parameter.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param pageToken the token indicating which page of results to fetch
     * @param size      the maximum number of users to return (default is 3)
     * @param query     an optional search term to filter the user list
     * @return a {@link ResponseEntity} containing a {@link UserPageResponse} with the requested users and pagination
     * details
     */
    @GetMapping
    public ResponseEntity<UserPageResponse> getOrgUsers(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(required = false) String query) {
        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getWorkspaceUsersPaged(adminEmail, pageToken, size, query));
    }

    /**
     * Retrieves a list of users who currently do not have Two-Factor Authentication (2FA) enforced or enabled. <p>
     *
     * This endpoint is primarily used by the notification system to alert administrators of potential security
     * vulnerabilities within the organization's user base.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link UsersWithoutTwoFactorResponse} with the non-compliant users
     */
    @GetMapping("/without-two-factor")
    public ResponseEntity<UsersWithoutTwoFactorResponse> getUsersWithoutTwoFactor(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getUsersWithoutTwoFactor(adminEmail));
    }

    /**
     * Retrieves a high-level overview and security metrics regarding the organization's users. <p>
     *
     * This includes aggregated data such as total active users, suspended accounts, and 2FA adoption rates. The
     * response is tailored based on the authenticated user's disabled security preferences.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link UserOverviewResponse} with the aggregated user metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getUsersPageOverview(adminEmail, preferenceService.getDisabledPreferenceKeys(adminEmail)));
    }

    /**
     * Forces a manual refresh of the Google Workspace users cache. <p>
     *
     * This endpoint is useful when a user want to immediately synchronize the local system with the latest
     * users data from Google Workspace, bypassing the scheduled caching intervals.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} confirming that the cache was successfully refreshed
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String adminEmail = jwtService.validateInternalToken(token);
        googleUserService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
