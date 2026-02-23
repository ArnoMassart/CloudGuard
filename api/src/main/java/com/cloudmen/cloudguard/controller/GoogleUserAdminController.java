package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.UserPageResponse;
import com.cloudmen.cloudguard.service.GoogleUserAdminService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/users")
public class GoogleUserAdminController {
    private final JwtService jwtService;
    private final GoogleUserAdminService googleUserAdminService;

    public GoogleUserAdminController(JwtService jwtService, GoogleUserAdminService googleUserAdminService) {
        this.jwtService = jwtService;
        this.googleUserAdminService = googleUserAdminService;
    }

    @GetMapping
    public ResponseEntity<UserPageResponse> getOrgUsers(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String query) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserAdminService.getWorkspaceUsersPaged(adminEmail, pageToken, size, query));
    }

    @GetMapping("/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserAdminService.getUsersPageOverview(adminEmail));
    }
}
