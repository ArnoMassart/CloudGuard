package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminSecurityKeysResponse;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/admin-security-keys")
public class AdminSecurityKeysController {

    private final AdminSecurityKeysService adminSecurityKeysService;
    private final JwtService jwtService;

    public AdminSecurityKeysController(AdminSecurityKeysService adminSecurityKeysService, JwtService jwtService) {
        this.adminSecurityKeysService = adminSecurityKeysService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<AdminSecurityKeysResponse> getAdminsWithSecurityKeys(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(adminSecurityKeysService.getAdminsWithSecurityKeys(email));
    }
}
