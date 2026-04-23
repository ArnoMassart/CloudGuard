package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AppPasswordController;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordPageResponse;
import com.cloudmen.cloudguard.dto.apppasswords.UserAppPasswordsDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AppPasswordsService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP integration tests: {@link AuthInterceptor}, controller wiring, and exception mapping. */
@WebMvcTest(
        controllers = AppPasswordController.class,
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
        AppPasswordControllerIT.MessageSourceTestConfig.class
})
class AppPasswordControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    /** Aangepast naar false zodat deze exact matcht met de controller (IS_TESTMODE = false) */
    private static final boolean TEST_MODE = false;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppPasswordsService appPasswordsService;

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
    void getAppPasswords_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/google/app-passwords").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getAppPasswords_withCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        var user =
                new UserAppPasswordsDto(
                        "u1", "Alice", "alice@acme.com", "USER", true, List.of());
        when(appPasswordsService.getAppPasswordsPaged(
                eq("admin@acme.com"), isNull(), eq(10), isNull(), eq(TEST_MODE)))
                .thenReturn(new AppPasswordPageResponse(List.of(user), "next"));

        mockMvc.perform(
                        get("/api/google/app-passwords")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.users[0].email").value("alice@acme.com"))
                .andExpect(jsonPath("$.nextPageToken").value("next"));
    }

    @Test
    void getAppPasswords_passesPageTokenSizeAndQuery() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(appPasswordsService.getAppPasswordsPaged(
                "admin@acme.com", "pt-1", 20, "mail", TEST_MODE))
                .thenReturn(new AppPasswordPageResponse(List.of(), null));

        mockMvc.perform(
                        get("/api/google/app-passwords")
                                .contextPath("/api")
                                .param("pageToken", "pt-1")
                                .param("size", "20")
                                .param("query", "mail")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(appPasswordsService)
                .getAppPasswordsPaged("admin@acme.com", "pt-1", 20, "mail", TEST_MODE);
    }

    @Test
    void getOverview_callsPreferencesAndReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(preferenceService.getDisabledPreferenceKeys("admin@acme.com"))
                .thenReturn(Set.of("pref-a"));
        var breakdown =
                new SecurityScoreBreakdownDto(
                        75,
                        "ok",
                        List.of(
                                new SecurityScoreFactorDto(
                                        "t", "d", 10, 20, "low")));
        when(appPasswordsService.getOverview(
                "admin@acme.com", TEST_MODE, Set.of("pref-a")))
                .thenReturn(new AppPasswordOverviewResponse(true, 5, 2, 75, breakdown));

        mockMvc.perform(
                        get("/api/google/app-passwords/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.totalAppPasswords").value(5))
                .andExpect(jsonPath("$.securityScore").value(75));

        verify(preferenceService).getDisabledPreferenceKeys("admin@acme.com");
    }

    @Test
    void postRefresh_validatesTokenAndRefreshesCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        mockMvc.perform(
                        post("/api/google/app-passwords/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(appPasswordsService).forceRefreshCache("admin@acme.com");
    }
}