package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.UserOrgDetail;
import com.cloudmen.cloudguard.service.GoogleAdminService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<List<UserOrgDetail>> getOrgUsers(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleAdminService.getAllWorkspaceUsers(email));
    }
}
