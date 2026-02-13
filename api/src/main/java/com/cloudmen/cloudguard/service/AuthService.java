package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.RequestCookieDto;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public ResponseCookie createEmptyCookie(RequestCookieDto requestCookieDto) {
        return ResponseCookie.from(requestCookieDto.name, "")
                .path(requestCookieDto.path)
                .domain(requestCookieDto.domain)
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();
    }
}
