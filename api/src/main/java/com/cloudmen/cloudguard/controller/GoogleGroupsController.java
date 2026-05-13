package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupPageResponse;
import com.cloudmen.cloudguard.service.GoogleGroupsService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP surface for Google Workspace <strong>groups</strong>: paginated directory data, aggregated overview with org
 * security preferences, and explicit cache refresh. All endpoints expect the {@code AuthToken} cookie and delegate
 * authentication to {@link JwtService#validateInternalToken(String)}.
 */
@RestController
@RequestMapping("/google/groups")
public class GoogleGroupsController {
    private final GoogleGroupsService googleGroupsService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * @param googleGroupsService domain logic and cache reads for groups
     * @param jwtService          validates the session cookie into the logged-in email
     * @param preferenceService   supplies disabled preference keys for overview scoring behaviour
     */
    public GoogleGroupsController(
            GoogleGroupsService googleGroupsService,
            JwtService jwtService,
            UserSecurityPreferenceService preferenceService) {
        this.googleGroupsService = googleGroupsService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Lists one page of groups with optional name/email substring filter and cursor pagination.
     */
    @GetMapping
    public ResponseEntity<GroupPageResponse> getOrgGroups(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsService.getGroupsPaged(loggedInEmail, query, pageToken, size));
    }

    /**
     * Returns aggregate counts, security score, breakdown, and preference warnings for the groups section.
     */
    @GetMapping("/overview")
    public ResponseEntity<GroupOverviewResponse> getGroupsOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(
                googleGroupsService.getGroupsOverview(loggedInEmail, preferenceService.getDisabledPreferenceKeys(loggedInEmail)));
    }

    /**
     * Forces the tenant’s Google Groups cache to reload from Google on the next read path used by this controller.
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        googleGroupsService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
