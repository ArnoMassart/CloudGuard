package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/app-passwords")
public class AppPasswordController {
    private final AppPasswordsService appPasswordsService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    public AppPasswordController(AppPasswordsService appPasswordsService, JwtService jwtService,
                                 UserSecurityPreferenceService preferenceService) {
        this.appPasswordsService = appPasswordsService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    @GetMapping()
    public AppPasswordPageResponse getAppPasswords(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return appPasswordsService.getAppPasswordsPaged(loggedInEmail, pageToken, size, query);
    }

    @GetMapping("/overview")
    public AppPasswordOverviewResponse getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return appPasswordsService.getOverview(loggedInEmail, preferenceService.getDisabledPreferenceKeys(loggedInEmail));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        appPasswordsService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
