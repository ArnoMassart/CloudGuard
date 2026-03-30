package com.cloudmen.cloudguard.interceptor;

import com.cloudmen.cloudguard.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Cookie[] cookies = request.getCookies();

        boolean hasToken = cookies != null && Arrays.stream(cookies)
                .anyMatch(c -> "AuthToken".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty());
        if (!hasToken) {
            throw new UnauthorizedException("Geen geldig authenticatietoken gevonden.");
        }

        return true;
    }
}
