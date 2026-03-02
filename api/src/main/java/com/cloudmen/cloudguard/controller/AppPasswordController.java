package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.passwords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.passwords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/app-passwords")
public class AppPasswordController {
    private final AppPasswordsService appPasswordsService;
    private final JwtService jwtService;

    public AppPasswordController(AppPasswordsService appPasswordsService, JwtService jwtService) {
        this.appPasswordsService = appPasswordsService;
        this.jwtService = jwtService;
    }

    @GetMapping()
    public AppPasswordPageResponse getAppPasswords(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "10") int size) {
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getAppPasswordsPaged(email, pageToken, size);
    }

    @GetMapping("/overview")
    public AppPasswordOverviewResponse getOverview(@CookieValue(name="AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getOverview(email);
    }
}
