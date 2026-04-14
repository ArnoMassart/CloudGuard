package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.GoogleDeviceController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.DevicePageResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.GoogleDeviceService;
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
        controllers = GoogleDeviceController.class,
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
        GoogleDeviceControllerIT.MessageSourceTestConfig.class
})
public class GoogleDeviceControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleDeviceService googleDeviceService;

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
    void getDevices_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/devices")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDevices_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of());

        DevicePageResponse mockResponse = new DevicePageResponse(List.of(), "next-page");
        when(googleDeviceService.getDevicesPaged(
                eq(EMAIL), isNull(), eq(5), isNull(), isNull(), eq(Set.of())))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/devices")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextPageToken").value("next-page"));

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(googleDeviceService).getDevicesPaged(EMAIL, null, 5, null, null, Set.of());
    }

    @Test
    void getDevices_passesAllParamsToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of("pref1"));

        DevicePageResponse mockResponse = new DevicePageResponse(List.of(), null);
        when(googleDeviceService.getDevicesPaged(
                EMAIL, "page-token", 20, "APPROVED", "Android", Set.of("pref1")))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/google/devices")
                                .contextPath("/api")
                                .param("pageToken", "page-token")
                                .param("size", "20")
                                .param("status", "APPROVED")
                                .param("deviceType", "Android")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(googleDeviceService).getDevicesPaged(EMAIL, "page-token", 20, "APPROVED", "Android", Set.of("pref1"));
    }

    // =========================================================================================
    // GET DEVICE TYPES TESTS
    // =========================================================================================

    @Test
    void getDeviceTypes_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/devices/types")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDeviceTypes_withValidCookie_returnsList() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(googleDeviceService.getUniqueDeviceTypes(EMAIL)).thenReturn(List.of("Android", "iOS", "Windows"));

        mockMvc.perform(
                        get("/api/google/devices/types")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("Android"))
                .andExpect(jsonPath("$[1]").value("iOS"))
                .andExpect(jsonPath("$[2]").value("Windows"));

        verify(googleDeviceService).getUniqueDeviceTypes(EMAIL);
    }

    // =========================================================================================
    // GET OVERVIEW TESTS
    // =========================================================================================

    @Test
    void getOverview_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/devices/overview")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_withValidCookie_returnsOverviewJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(preferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of("pref1"));

        DeviceOverviewResponse mockOverview = mock(DeviceOverviewResponse.class);
        when(googleDeviceService.getDevicesPageOverview(EMAIL, Set.of("pref1"))).thenReturn(mockOverview);

        mockMvc.perform(
                        get("/api/google/devices/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService).getDisabledPreferenceKeys(EMAIL);
        verify(googleDeviceService).getDevicesPageOverview(EMAIL, Set.of("pref1"));
    }

    // =========================================================================================
    // POST REFRESH TESTS
    // =========================================================================================

    @Test
    void refreshDevicesCache_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/google/devices/refresh")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshDevicesCache_withValidCookie_callsServiceAndReturnsMessage() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        mockMvc.perform(
                        post("/api/google/devices/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(googleDeviceService).forceRefreshCache(EMAIL);
    }
}
