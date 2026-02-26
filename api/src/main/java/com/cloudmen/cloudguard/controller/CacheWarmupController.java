package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.CacheWarmupService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache-warmup")
public class CacheWarmupController {
    private final CacheWarmupService warmupService;
    private final JwtService jwtService;

    public CacheWarmupController(CacheWarmupService warmupService, JwtService jwtService) {
        this.warmupService = warmupService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<Void> triggerWarmup(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

            String adminEmail = jwtService.validateInternalToken(token);

            // Start het achtergrondproces!
            warmupService.warmupAllCachesAsync(adminEmail);

            // 202 ACCEPTED betekent: "Ik heb je verzoek ontvangen en ga er op de achtergrond mee aan de slag"
            return ResponseEntity.accepted().build();
    }
}
