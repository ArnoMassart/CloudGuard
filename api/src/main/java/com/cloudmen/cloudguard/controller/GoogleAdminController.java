package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.UserOrgDetail;
import com.cloudmen.cloudguard.dto.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.UserPageResponse;
import com.cloudmen.cloudguard.service.GoogleAdminService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/google")
public class GoogleAdminController {
    private final GoogleAdminService googleAdminService;
    private final JwtService jwtService;

    public GoogleAdminController(GoogleAdminService googleAdminService, JwtService jwtService) {
        this.googleAdminService = googleAdminService;
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

        return ResponseEntity.ok(googleAdminService.getWorkspaceUsersPaged(adminEmail, pageToken, size, query));
    }

    @GetMapping("/users/overview")
    public ResponseEntity<UserOverviewResponse> getUsersPageOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleAdminService.getUsersPageOverview(adminEmail));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupOrgDetail>> getOrgGroups(@CookieValue(name="AuthToken",required=false) String token){
        if(token == null || token.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleAdminService.getAllWorkspaceGroups(email));
    }
}
