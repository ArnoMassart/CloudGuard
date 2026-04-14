package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.GoogleUsersController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.dto.users.UsersWithoutTwoFactorResponse;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.GoogleUsersService;
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
        controllers = GoogleUsersController.class,
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
        GoogleUsersControllerIT.MessageSourceTestConfig.class
})
public class GoogleUsersControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleUsersService googleUserService;

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
    // GET USERS TESTS
    // =========================================================================================

    @Test
    void getOrgUsers_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/users")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrgUsers_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        UserPageResponse mockResponse = new UserPageResponse(List.of(), "next-page");
        // default size is 3 volgens de @RequestParam in de controller
        when(googleUserService.getWorkspaceUsersPaged(eq(EMAIL), isNull(), eq(3), isNull()))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/users")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextPageToken").value("next-page"));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(googleUserService).getWorkspaceUsersPaged(EMAIL, null, 3, null);
    }

    @Test
    void getOrgUsers_passesAllParamsToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        UserPageResponse mockResponse = new UserPageResponse(List.of(), null);
        when(googleUserService.getWorkspaceUsersPaged(EMAIL, "pt", 20, "searchQuery"))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/users")
                                .contextPath("/api")
                                .param("pageToken", "pt")
                                .param("size", "20")
                                .param("query", "searchQuery")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(googleUserService).getWorkspaceUsersPaged(EMAIL, "pt", 20, "searchQuery");
    }

    // =========================================================================================
    // GET USERS WITHOUT TWO FACTOR TESTS
    // =========================================================================================

    @Test
    void getUsersWithoutTwoFactor_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/users/without-two-factor")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsersWithoutTwoFactor_withValidCookie_returnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        UsersWithoutTwoFactorResponse mockResponse = new UsersWithoutTwoFactorResponse(List.of());
        when(googleUserService.getUsersWithoutTwoFactor(EMAIL)).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/users/without-two-factor")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(googleUserService).getUsersWithoutTwoFactor(EMAIL);
    }

    // =========================================================================================
    // GET OVERVIEW TESTS
    // =========================================================================================

    @Test
    void getUsersPageOverview_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/users/overview")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsersPageOverview_withValidCookie_callsServiceAndReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of("pref1"));

        UserOverviewResponse mockResponse = mock(UserOverviewResponse.class);
        when(googleUserService.getUsersPageOverview(EMAIL, Set.of("pref1"))).thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/users/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(googleUserService).getUsersPageOverview(EMAIL, Set.of("pref1"));
    }

    // =========================================================================================
    // POST REFRESH TESTS
    // =========================================================================================

    @Test
    void refreshUsersCache_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/google/users/refresh")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshUsersCache_withValidCookie_callsServiceAndReturnsMessage() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        mockMvc.perform(
                        post("/api/google/users/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(googleUserService).forceRefreshCache(EMAIL);
    }
}
