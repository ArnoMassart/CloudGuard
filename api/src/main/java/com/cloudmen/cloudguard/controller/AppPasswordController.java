package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.passwords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.passwords.UserAppPasswordsDto;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<UserAppPasswordsDto> getAppPasswords(@CookieValue(name="AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getAppPasswords(email);
    }

    @GetMapping("/overview")
    public AppPasswordOverviewResponse getOverview(@CookieValue(name="AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return appPasswordsService.getOverview(email);
    }
}
