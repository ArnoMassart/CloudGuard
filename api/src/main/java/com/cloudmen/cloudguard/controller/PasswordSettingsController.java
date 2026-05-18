package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.password.PasswordSettingsOverviewResponse;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for the password-settings workspace module: OU-level password policies from the Policy API,
 * 2-step verification rollups, users flagged for password change, admin security-key gaps, and the composite score.
 * Endpoints authenticate via the {@code AuthToken} cookie (validated through {@link JwtService}).
 *
 * @see PasswordSettingsService
 */
@RestController
@RequestMapping("/google/password-settings")
public class PasswordSettingsController {
    private final PasswordSettingsService passwordSettingsService;
    private final JwtService jwtService;

    /**
     * @param passwordSettingsService aggregates Directory users, Policy API password rules, admin keys, and scoring
     * @param jwtService              resolves session cookie to primary email for tenant-scoped data
     */
    public PasswordSettingsController(PasswordSettingsService passwordSettingsService, JwtService jwtService) {
        this.passwordSettingsService = passwordSettingsService;
        this.jwtService = jwtService;
    }

    /** Returns cached password-settings overview for the authenticated user’s tenant (one-hour Caffeine TTL per user). */
    @GetMapping
    public ResponseEntity<PasswordSettingsOverviewResponse> getPasswordSettings(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(passwordSettingsService.getPasswordSettings(email));
    }

    /**
     * Drops password-settings cache for this user and triggers refresh of dependent caches (policy API, users,
     * org units, admin security keys).
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        passwordSettingsService.forceRefreshCache(email);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
