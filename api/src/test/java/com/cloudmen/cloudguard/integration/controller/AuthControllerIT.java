package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Import(
        {
                SecurityConfig.class,
                WebConfig.class,
                AuthInterceptor.class,
                GlobalExceptionHandler.class,
                AuthControllerIT.MessageSourceTestConfig.class
        }
)
public class AuthControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserSecurityPreferenceService preferenceService;

    static class MessageSourceTestConfig {
        @Bean
        org.springframework.context.MessageSource messageSource() {
            ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
            ms.setBasenames("messages");
            ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
            ms.setFallbackToSystemLocale(false);
            return ms;
        }
    }

    private UserDto createTestUser() {
        return new UserDto(
                "admin@acme.com",
                "Admin",
                "User",
                "url",
                List.of("Admin"),
                LocalDateTime.now()
        );
    }

    @Test
    void logout_withValidCookie_clearsCookieAndReturns200() throws Exception {
        ResponseCookie emptyCookie = ResponseCookie.from(AUTH_COOKIE, "").maxAge(0).path("/").build();

        when(authService.createEmptyCookie()).thenReturn(emptyCookie);
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(cookie().value(AUTH_COOKIE, ""))
                .andExpect(cookie().maxAge(AUTH_COOKIE, 0))
                .andExpect(cookie().path(AUTH_COOKIE, "/"));

        verify(authService).createEmptyCookie();
    }

    @Test
    void logout_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                post("/api/auth/logout")
                        .contextPath("/api")
        ).andExpect(status().isUnauthorized())
                .andExpect(content().string("An error occured. Please sign in again."));
    }

    @Test
    void checkSession_withValidCookie_returnsUserJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(authService.validateSession(VALID_TOKEN)).thenReturn(createTestUser());

        mockMvc.perform(
                        get("/api/auth/check-session")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("admin@acme.com"))
                .andExpect(jsonPath("$.firstName").value("Admin"));

        verify(authService).validateSession(VALID_TOKEN);
    }

    @Test
    void checkSession_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/auth/check-session")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("An error occured. Please sign in again."));

        // We verifiëren authService niet, want het request bereikt de controller nooit
    }

    @Test
    void getCurrentUser_withValidCookie_returnsUserJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(authService.getCurrentUser(VALID_TOKEN)).thenReturn(Optional.of(createTestUser()));

        mockMvc.perform(
                        get("/api/auth/me")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("admin@acme.com"));

        verify(authService).getCurrentUser(VALID_TOKEN);
    }

    @Test
    void getCurrentUser_withoutCookie_returns401() throws Exception {
        when(authService.getCurrentUser(null)).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/auth/me")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("An error occured. Please sign in again."));
    }

    @Test
    void getCurrentUser_userNotFound_returns401() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(authService.getCurrentUser(VALID_TOKEN)).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/auth/me")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isUnauthorized());

        verify(authService).getCurrentUser(VALID_TOKEN);
    }

    // =========================================================================================
    // LOGIN TESTS
    // =========================================================================================

    @Test
    void googleLogin_withValidToken_returnsUserJsonAndSetsCookie() throws Exception {
        String externalToken = "external-google-id-token";
        TokenRequestDto requestDto = new TokenRequestDto();
        requestDto.setToken(externalToken);

        ResponseCookie sessionCookie = ResponseCookie.from(AUTH_COOKIE, VALID_TOKEN)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(86400)
                .sameSite("Strict")
                .build();

        LoginResult loginResult = new LoginResult(sessionCookie, createTestUser());

        when(authService.processLogin(eq(externalToken))).thenReturn(loginResult);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, sessionCookie.toString()))
                .andExpect(jsonPath("$.email").value("admin@acme.com"));

        verify(authService).processLogin(externalToken);
    }
}
