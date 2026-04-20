package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.preferences.PreferencesResponse;
import com.cloudmen.cloudguard.dto.preferences.SectionPreferencesRequest;
import com.cloudmen.cloudguard.dto.preferences.SetPreferenceRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/preferences")
public class UserSecurityPreferenceController {

    private final UserSecurityPreferenceService preferenceService;
    private final JwtService jwtService;
    private final PasswordSettingsService passwordSettingsService;

    public UserSecurityPreferenceController(UserSecurityPreferenceService preferenceService, JwtService jwtService,
                                            PasswordSettingsService passwordSettingsService) {
        this.preferenceService = preferenceService;
        this.jwtService = jwtService;
        this.passwordSettingsService = passwordSettingsService;
    }

    /**
     * Get all security preferences: boolean toggles plus per–DNS-type effective importance (SPF, DKIM, …).
     */
    @GetMapping
    public ResponseEntity<PreferencesResponse> getAllPreferences(
            @CookieValue(name = "AuthToken") String token) {
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(preferenceService.getPreferencesResponse(userId));
    }

    /**
     * Get preferences for a specific section.
     */
    @GetMapping("/{section}")
    public ResponseEntity<Map<String, Boolean>> getSectionPreferences(
            @CookieValue(name = "AuthToken") String token,
            @PathVariable String section) {
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(preferenceService.getPreferencesForSection(userId, section));
    }

    /**
     * Get disabled preference keys as a list of "section:preferenceKey".
     * Used by frontend to filter warnings/notifications.
     */
    @GetMapping("/disabled")
    public ResponseEntity<List<String>> getDisabledKeys(
            @CookieValue(name = "AuthToken") String token) {
        String userId = jwtService.validateInternalToken(token);
        var keys = preferenceService.getDisabledPreferenceKeys(userId).stream().sorted().toList();
        return ResponseEntity.ok(keys);
    }

    /**
     * Set a single preference.
     */
    @PutMapping
    public ResponseEntity<?> setPreference(
            @CookieValue(name = "AuthToken") String token,
            @RequestBody SetPreferenceRequest request) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        preferenceService.setPreference(loggedInEmail, request.section(), request.preferenceKey(), request.enabled(), request.value());
        if ("password-settings".equals(request.section())) {
            passwordSettingsService.forceRefreshCache(loggedInEmail);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Set multiple preferences for a section.
     */
    @PutMapping("/section")
    public ResponseEntity<?> setSectionPreferences(
            @CookieValue(name = "AuthToken") String token,
            @RequestBody SectionPreferencesRequest request) {
        String userId = jwtService.validateInternalToken(token);
        preferenceService.setSectionPreferences(userId, request.section(), request.preferences());
        if ("password-settings".equals(request.section())) {
            passwordSettingsService.forceRefreshCache(userId);
        }
        return ResponseEntity.ok().build();
    }
}
