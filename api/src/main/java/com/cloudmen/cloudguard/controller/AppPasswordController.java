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

    private static final boolean IS_TESTMODE = false;

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
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getAppPasswordsPaged(email, pageToken, size, query, IS_TESTMODE);
    }

    @GetMapping("/overview")
    public AppPasswordOverviewResponse getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getOverview(email, IS_TESTMODE, preferenceService.getDisabledPreferenceKeys(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(@CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        appPasswordsService.forceRefreshCache(email);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
