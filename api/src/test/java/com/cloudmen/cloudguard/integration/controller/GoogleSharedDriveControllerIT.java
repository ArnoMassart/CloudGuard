package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.GoogleSharedDriveController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.drives.SharedDrivePageResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.GoogleSharedDriveService;
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
        controllers = GoogleSharedDriveController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.port=8080", "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        })
@Import({
        SecurityConfig.class,
        WebConfig.class,
        AuthInterceptor.class,
        GlobalExceptionHandler.class,
        GoogleSharedDriveControllerIT.MessageSourceTestConfig.class
})
public class GoogleSharedDriveControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleSharedDriveService driveService;

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
    // GET DRIVES TESTS
    // =========================================================================================

    @Test
    void getDrives_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/drives")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDrives_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        SharedDrivePageResponse mockResponse = new SharedDrivePageResponse(List.of(), "next-page");

        // Gebruik anyInt() voor de 'size' parameter om mismatches met default values te voorkomen
        when(driveService.getSharedDrivesPaged(eq(EMAIL), isNull(), anyInt(), isNull()))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/drives")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextPageToken").value("next-page"));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        // Verifieer ook hier met anyInt()
        verify(driveService).getSharedDrivesPaged(eq(EMAIL), isNull(), anyInt(), isNull());
    }

    @Test
    void getDrives_passesAllParamsToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        SharedDrivePageResponse mockResponse = new SharedDrivePageResponse(List.of(), null);
        when(driveService.getSharedDrivesPaged(EMAIL, "pt", 20, "searchQuery"))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/drives")
                                .contextPath("/api")
                                .param("pageToken", "pt")
                                .param("size", "20")
                                .param("query", "searchQuery")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(driveService).getSharedDrivesPaged(EMAIL, "pt", 20, "searchQuery");
    }

    // =========================================================================================
    // GET OVERVIEW TESTS
    // =========================================================================================

    @Test
    void getOverview_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/drives/overview")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_withEmptyCookie_returns401() throws Exception {
        // Dit triggert de `if (token == null || token.isEmpty())` block in de controller
        mockMvc.perform(
                        get("/api/google/drives/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_withValidCookie_callsServiceAndReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of("pref1"));

        SharedDriveOverviewResponse mockResponse = mock(SharedDriveOverviewResponse.class);
        when(driveService.getDrivesPageOverview(EMAIL, Set.of("pref1"))).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/drives/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(driveService).getDrivesPageOverview(EMAIL, Set.of("pref1"));
    }

    // =========================================================================================
    // POST REFRESH TESTS
    // =========================================================================================

    @Test
    void refreshDrivesCache_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/google/drives/refresh")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshDrivesCache_withValidCookie_callsServiceAndReturnsMessage() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        mockMvc.perform(
                        post("/api/google/drives/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(driveService).forceRefreshCache(EMAIL);
    }
}
