package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.GoogleOAuthController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthPagedResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.GoogleOAuthService;
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
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GoogleOAuthController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        })
@Import({
        SecurityConfig.class,
        WebConfig.class,
        AuthInterceptor.class,
        GlobalExceptionHandler.class,
        GoogleOAuthControllerIT.MessageSourceTestConfig.class
})
public class GoogleOAuthControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOAuthService oAuthService;

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

    // =========================================================================================
    // GET TOKENS TESTS
    // =========================================================================================

    @Test
    void getTokens_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/oAuth")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTokens_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        OAuthPagedResponse mockResponse = new OAuthPagedResponse(List.of(), "next-page", 0, 0, 0);
        when(oAuthService.getOAuthPaged(eq(EMAIL), isNull(), eq(3), isNull(), isNull()))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/oAuth")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextPageToken").value("next-page"));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(oAuthService).getOAuthPaged(EMAIL, null, 3, null, null);
    }

    @Test
    void getTokens_passesAllParamsToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        OAuthPagedResponse mockResponse = new OAuthPagedResponse(List.of(), null, 1, 1, 0);
        when(oAuthService.getOAuthPaged(EMAIL, "pt", 10, "appname", "high"))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/oAuth")
                                .contextPath("/api")
                                .param("pageToken", "pt")
                                .param("size", "10")
                                .param("query", "appname")
                                .param("risk", "high")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(oAuthService).getOAuthPaged(EMAIL, "pt", 10, "appname", "high");
    }

    // =========================================================================================
    // GET OVERVIEW TESTS
    // =========================================================================================

    @Test
    void getOverview_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/oAuth/overview")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_withValidCookie_callsServiceAndReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of("pref1"));

        OAuthOverviewResponse mockResponse = mock(OAuthOverviewResponse.class);
        when(oAuthService.getOAuthPageOverview(EMAIL, Set.of("pref1"))).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/oAuth/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(oAuthService).getOAuthPageOverview(EMAIL, Set.of("pref1"));
    }

    // =========================================================================================
    // POST REFRESH TESTS
    // =========================================================================================

    @Test
    void refreshOAuthCache_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/google/oAuth/refresh")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshOAuthCache_withValidCookie_callsServiceAndReturnsMessage() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        mockMvc.perform(
                        post("/api/google/oAuth/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(oAuthService).forceRefreshCache(EMAIL);
    }
}
