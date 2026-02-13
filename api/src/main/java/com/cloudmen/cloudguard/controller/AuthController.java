package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.RequestCookieDto;
import com.cloudmen.cloudguard.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService = new AuthService();

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RequestCookieDto cookieDto) {
        ResponseCookie cookie = authService.createEmptyCookie(cookieDto);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}
