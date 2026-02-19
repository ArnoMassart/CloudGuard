package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.GroupPageResponse;
import com.cloudmen.cloudguard.dto.GroupSettingsDto;
import com.cloudmen.cloudguard.dto.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.UserPageResponse;
import com.cloudmen.cloudguard.service.GoogleGroupsAdminService;
import com.cloudmen.cloudguard.service.GoogleUserAdminService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/google")
public class GoogleAdminController {
    private final GoogleUserAdminService googleUserAdminService;
    private final GoogleGroupsAdminService googleGroupsAdminService;
    private final JwtService jwtService;

    public GoogleAdminController(
            GoogleUserAdminService googleUserAdminService,
            GoogleGroupsAdminService googleGroupsAdminService,
            JwtService jwtService) {
        this.googleUserAdminService = googleUserAdminService;
        this.googleGroupsAdminService = googleGroupsAdminService;
        this.jwtService = jwtService;
    }

    @GetMapping("/users")
    public ResponseEntity<UserPageResponse> getOrgUsers(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserAdminService.getWorkspaceUsersPaged(adminEmail, pageToken, size, query));
    }

    @GetMapping("/users/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleUserAdminService.getUsersPageOverview(adminEmail));
    }

    @GetMapping("/groups/overview")
    public ResponseEntity<GroupOverviewResponse> getGroupsOverview(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsAdminService.getGroupsOverview(email));
    }

    @GetMapping("/groups")
    public ResponseEntity<GroupPageResponse> getOrgGroups(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsAdminService.getGroupsPaged(email, query, pageToken, size));
    }

    @GetMapping("/groups/settings")
    public ResponseEntity<GroupSettingsDto> getGroupSettings(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam String groupEmail) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleGroupsAdminService.getGroupSettings(email, groupEmail));
    }
}
