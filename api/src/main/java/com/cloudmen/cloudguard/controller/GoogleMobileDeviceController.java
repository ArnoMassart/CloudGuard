package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.devices.MobileDeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.MobileDevicePageResponse;
import com.cloudmen.cloudguard.service.GoogleMobileDeviceService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/google/devices")
public class GoogleMobileDeviceController {
    private final GoogleMobileDeviceService googleMobileDeviceService;
    private final JwtService jwtService;

    private static final boolean isTestMode = true;

    public GoogleMobileDeviceController(GoogleMobileDeviceService googleMobileDeviceService, JwtService jwtService) {
        this.googleMobileDeviceService = googleMobileDeviceService;
        this.jwtService = jwtService;
    }

    @GetMapping()
    public ResponseEntity<MobileDevicePageResponse> getMobileDevices(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType
            ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleMobileDeviceService.getMobileDevicesPaged(loggedInEmail, pageToken, size, status, deviceType, isTestMode));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getDeviceTypes(@CookieValue(name = "AuthToken", required = false) String token
                                                       ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleMobileDeviceService.getUniqueDeviceTypes(loggedInEmail, isTestMode));
    }

    @GetMapping("/overview")
    public ResponseEntity<MobileDeviceOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleMobileDeviceService.getMobileDevicesPageOverview(loggedInEmail, isTestMode));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        googleMobileDeviceService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
