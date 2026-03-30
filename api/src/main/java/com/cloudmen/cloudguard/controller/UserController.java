package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtService jwtService;
    private final UserService userService;

    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @GetMapping("/language")
    public ResponseEntity<String> getLanguage(@CookieValue(name = "AuthToken", required = false) String token) {
        String adminEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getLanguage(adminEmail));
    }

    @PostMapping("/language")
    public ResponseEntity<Void> updateLanguage(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody Map<String, String> request) {
        String adminEmail = jwtService.validateInternalToken(token);

        String newLanguage = request.get("language");

        userService.updateLanguage(adminEmail, newLanguage);

        return ResponseEntity.ok().build();
    }
}
