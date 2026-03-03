package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.licenses.LicensePageResponse;
import com.cloudmen.cloudguard.service.GoogleLicenseService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/google/license")
public class LicenseController {
    private final GoogleLicenseService licenseService;
    private final JwtService jwtService;

    public LicenseController(GoogleLicenseService licenseService, JwtService jwtService) {
        this.licenseService = licenseService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<LicensePageResponse> getLicenses(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(licenseService.getLicenses(adminEmail));
    }
}
