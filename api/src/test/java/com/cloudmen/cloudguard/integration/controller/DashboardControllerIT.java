package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.DashboardController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.DashboardService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DashboardController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.port=8080", "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Import(
        {
                SecurityConfig.class,
                WebConfig.class,
                AuthInterceptor.class,
                GlobalExceptionHandler.class,
                DashboardControllerIT.MessageSourceTestConfig.class
        }
)
public class DashboardControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

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

    @Test
    void getDashboardData_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/dashboard")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboardData_withValidCookie_callsServiceAndReturnsOk() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        DashboardPageResponse mockResponse = mock(DashboardPageResponse.class);
        when(dashboardService.getDashboardSecurityScores(EMAIL)).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/dashboard")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(dashboardService).getDashboardSecurityScores(EMAIL);
    }

    // =========================================================================================
    // GET OVERVIEW TESTS
    // =========================================================================================

    @Test
    void getOverview_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/dashboard/overview")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_withValidCookie_callsServiceAndReturnsOk() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        DashboardOverviewResponse mockResponse = mock(DashboardOverviewResponse.class);
        when(dashboardService.getDashboardOverview(EMAIL)).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/dashboard/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(dashboardService).getDashboardOverview(EMAIL);
    }
}
