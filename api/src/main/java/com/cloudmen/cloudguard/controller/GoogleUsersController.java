package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.service.GoogleUsersService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/users")
public class GoogleUsersController {
    private final JwtService jwtService;
    private final GoogleUsersService googleUserService;


    public GoogleUsersController(JwtService jwtService, GoogleUsersService googleUserService ) {
        this.jwtService = jwtService;
        this.googleUserService = googleUserService;
    }

    @GetMapping
    public ResponseEntity<UserPageResponse> getOrgUsers(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(required = false) String query) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getWorkspaceUsersPaged(adminEmail, pageToken, size, query));
    }

    @GetMapping("/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserService.getUsersPageOverview(adminEmail));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        googleUserService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
