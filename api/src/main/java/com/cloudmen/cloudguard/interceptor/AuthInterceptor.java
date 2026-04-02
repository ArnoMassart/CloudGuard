package com.cloudmen.cloudguard.interceptor;

import com.cloudmen.cloudguard.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Arrays;
import java.util.Locale;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final MessageSource messageSource;

    public AuthInterceptor(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Cookie[] cookies = request.getCookies();

        boolean hasToken = cookies != null && Arrays.stream(cookies)
                .anyMatch(c -> "AuthToken".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty());
        if (!hasToken) {
            Locale locale = RequestContextUtils.getLocale(request);
            throw new UnauthorizedException(
                    messageSource.getMessage("api.auth.cookie_missing", null, locale));
        }

        return true;
    }
}
