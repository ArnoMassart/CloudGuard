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
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
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
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/without-two-factor")
    public ResponseEntity<UsersWithoutTwoFactorResponse> getUsersWithoutTwoFactor(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getUsersWithoutTwoFactor(adminEmail));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getUsersPageOverview(adminEmail, preferenceService.getDisabledPreferenceKeys(adminEmail)));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
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
