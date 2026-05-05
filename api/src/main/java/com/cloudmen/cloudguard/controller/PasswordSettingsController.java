package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.password.PasswordSettingsOverviewResponse;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/password-settings")
public class PasswordSettingsController {
    private final PasswordSettingsService passwordSettingsService;
    private final JwtService jwtService;

    public PasswordSettingsController(PasswordSettingsService passwordSettingsService, JwtService jwtService) {
        this.passwordSettingsService = passwordSettingsService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<PasswordSettingsOverviewResponse> getPasswordSettings(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(passwordSettingsService.getPasswordSettings(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        passwordSettingsService.forceRefreshCache(email);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
