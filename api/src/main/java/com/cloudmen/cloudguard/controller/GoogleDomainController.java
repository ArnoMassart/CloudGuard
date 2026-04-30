package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.service.GoogleDomainService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/google/domains")
public class GoogleDomainController {

    private final GoogleDomainService googleDomainService;
    private final JwtService jwtService;

    public GoogleDomainController(GoogleDomainService googleDomainService, JwtService jwtService) {
        this.googleDomainService = googleDomainService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<DomainDto>> getAllDomains(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleDomainService.getAllDomains(loggedInEmail));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        googleDomainService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
