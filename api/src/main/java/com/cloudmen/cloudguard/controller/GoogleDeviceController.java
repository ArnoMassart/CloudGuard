package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.DevicePageResponse;
import com.cloudmen.cloudguard.service.GoogleDeviceService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/google/devices")
public class GoogleDeviceController {
    private final GoogleDeviceService googleDeviceService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    public GoogleDeviceController(GoogleDeviceService googleDeviceService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.googleDeviceService = googleDeviceService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    @GetMapping()
    public ResponseEntity<DevicePageResponse> getDevices(
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

        Set<String> disabled = preferenceService.getDisabledPreferenceKeys(loggedInEmail);
        return ResponseEntity.ok(googleDeviceService.getDevicesPaged(loggedInEmail, pageToken, size, status, deviceType, disabled));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getDeviceTypes(@CookieValue(name = "AuthToken", required = false) String token
                                                       ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleDeviceService.getUniqueDeviceTypes(loggedInEmail));
    }

    @GetMapping("/overview")
    public ResponseEntity<DeviceOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleDeviceService.getDevicesPageOverview(loggedInEmail, preferenceService.getDisabledPreferenceKeys(loggedInEmail)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshDevicesCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        googleDeviceService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
