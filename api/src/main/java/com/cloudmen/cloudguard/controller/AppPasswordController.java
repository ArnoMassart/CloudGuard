package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for Google Workspace <strong>application-specific passwords</strong> (Admin SDK ASPs): paginated users
 * who still have ASPs, aggregate overview with security score, and cache refresh. Uses the {@code AuthToken} cookie.
 *
 * @see AppPasswordsService
 */
@RestController
@RequestMapping("/google/app-passwords")
public class AppPasswordController {
    private final AppPasswordsService appPasswordsService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * @param appPasswordsService lists ASPs and builds overview metrics from Directory
     * @param jwtService          validates session cookie into logged-in email
     * @param preferenceService   supplies disabled org preference keys for the overview overload
     */
    public AppPasswordController(
            AppPasswordsService appPasswordsService,
            JwtService jwtService,
            UserSecurityPreferenceService preferenceService) {
        this.appPasswordsService = appPasswordsService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Returns one page of users who have at least one app password, after optional name/email filter.
     *
     * @param pageToken stringified 1-based page index; absent means page 1
     * @param size      page size (clamped to {@code 1…100})
     */
    @GetMapping()
    public AppPasswordPageResponse getAppPasswords(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return appPasswordsService.getAppPasswordsPaged(loggedInEmail, pageToken, size, query);
    }

    /**
     * Aggregate ASP counts, user-with-ASP counts, weighted score, and breakdown for dashboards / section cards.
     */
    @GetMapping("/overview")
    public AppPasswordOverviewResponse getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return appPasswordsService.getOverview(loggedInEmail, preferenceService.getDisabledPreferenceKeys(loggedInEmail));
    }

    /** Forces a synchronous reload of the ASP snapshot for this viewer’s tenant cache key. */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        appPasswordsService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
