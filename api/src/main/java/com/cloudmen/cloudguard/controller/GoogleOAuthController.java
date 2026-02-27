package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.oAuth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.oAuth.OAuthPagedResponse;
import com.cloudmen.cloudguard.service.GoogleOAuthService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/oAuth")
public class GoogleOAuthController {
    private final GoogleOAuthService oAuthService;
    private final JwtService jwtService;

    public GoogleOAuthController(GoogleOAuthService oAuthService, JwtService jwtService) {
        this.oAuthService = oAuthService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<OAuthPagedResponse> getToken(@CookieValue(name = "AuthToken", required = false) String token,
                                                       @RequestParam(required = false) String pageToken,
                                                       @RequestParam(defaultValue = "3") int size,
                                                       @RequestParam(required = false) String query) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(oAuthService.getOAuthPaged(loggedInEmail, pageToken, size, query));
    }

    @GetMapping("/overview")
    public ResponseEntity<OAuthOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token
                                                          ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(oAuthService.getOAuthPageOverview(loggedInEmail));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshOAuthCache(@CookieValue(name = "AuthToken", required = false) String token
                                                          ) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        oAuthService.forceRefreshCache(loggedInEmail);

        return ResponseEntity.ok("Cache is succesvol vernieuwd!");

    }
}
