package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.cache.CacheWarmupService;
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
        String adminEmail = jwtService.validateInternalToken(token);

        warmupService.warmupAllCachesAsync(adminEmail);

        return ResponseEntity.accepted().build();
    }
}
