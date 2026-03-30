package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.service.GoogleSharedDriveService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/drives")
public class GoogleSharedDriveController {
    private final GoogleSharedDriveService driveService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    public GoogleSharedDriveController(GoogleSharedDriveService driveService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.driveService = driveService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<SharedDrivePageResponse> getDrives(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(driveService.getSharedDrivesPaged(loggedInEmail, pageToken, size, query));
    }

    @GetMapping("/overview")
    public ResponseEntity<SharedDriveOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(driveService.getDrivesPageOverview(adminEmail, preferenceService.getDisabledPreferenceKeys(adminEmail)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String adminEmail = jwtService.validateInternalToken(token);
        driveService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
