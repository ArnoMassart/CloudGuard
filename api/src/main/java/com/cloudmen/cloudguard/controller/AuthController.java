package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService = new AuthService();

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(name = "AuthToken", defaultValue = "no-token") String token) {
        if (token.equals("no-token")) {
            return ResponseEntity.status(401).body("Niet ingelogd");
        }
        ResponseCookie cookie = authService.createEmptyCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/check-session")
    public ResponseEntity<Void> checkSession(@CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Optioneel: valideer hier de token-inhoud (bijv. JWT verloopdatum)
        return ResponseEntity.ok().build();
    }
}
