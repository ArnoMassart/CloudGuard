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

/**
 * A Spring MVC interceptor responsible for enforcing basic authentication checks on incoming HTTP requests. <p>
 *
 * This interceptor ensures that any secured route is accessed by a client holding a valid, non-empty "AuthToken"
 * cookie. If the required cookie is missing or empty, the request is immediately rejected and an
 * {@link UnauthorizedException} is thrown, utilizing the request's locale to provide a translated error message.
 */
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
