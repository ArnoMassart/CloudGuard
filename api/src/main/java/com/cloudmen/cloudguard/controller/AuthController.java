package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(name = "AuthToken", defaultValue = "no-token") String token) {
        if (token.equals("no-token")) {
            return ResponseEntity.status(401).body("Niet ingelogd");
        }
        ResponseCookie cookie = authService.createEmptyCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/check-session")
    public ResponseEntity<UserDto> checkSession(@CookieValue(name = "AuthToken", required = false) String token) {
        UserDto user = authService.validateSession(token);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@CookieValue(name = "AuthToken", required = false) String token) {
        return authService.getCurrentUser(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> googleLogin(@RequestBody TokenRequestDto request) {
        LoginResult result = authService.processLogin(request.getToken());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.userDto());
    }
}
