package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupPageResponse;
import com.cloudmen.cloudguard.dto.groups.GroupSettingsDto;
import com.cloudmen.cloudguard.service.GoogleGroupsService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/groups")
public class GoogleGroupsController {
    private final GoogleGroupsService googleGroupsService;
    private final JwtService jwtService;

    public GoogleGroupsController(GoogleGroupsService googleGroupsService, JwtService jwtService) {
        this.googleGroupsService = googleGroupsService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<GroupPageResponse> getOrgGroups(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsService.getGroupsPaged(email, query, pageToken, size));
    }

    @GetMapping("/overview")
    public ResponseEntity<GroupOverviewResponse> getGroupsOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsService.getGroupsOverview(email));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        googleGroupsService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
