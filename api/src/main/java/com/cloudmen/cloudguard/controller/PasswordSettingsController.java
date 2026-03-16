package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<PasswordSettingsDto> getPasswordSettings(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(passwordSettingsService.getPasswordSettings(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = jwtService.validateInternalToken(token);
        passwordSettingsService.forceRefreshCache(email);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
