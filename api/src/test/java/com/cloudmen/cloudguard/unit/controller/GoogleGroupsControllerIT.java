package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.GoogleGroupsController;
import com.cloudmen.cloudguard.dto.groups.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupPageResponse;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.dto.preferences.SectionWarningsDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.GoogleGroupsService;
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
import java.util.Map;
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
        controllers = GoogleGroupsController.class,
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
    GoogleGroupsControllerIT.MessageSourceTestConfig.class
})
class GoogleGroupsControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleGroupsService googleGroupsService;

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
    void getGroups_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/google/groups").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getGroups_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        var group =
                new GroupOrgDetail(
                        "g1",
                        "admin",
                        "low",
                        List.of("t1"),
                        10,
                        0,
                        false,
                        "ALL",
                        "ALL");
        when(googleGroupsService.getGroupsPaged(
                        eq("admin@acme.com"), isNull(), isNull(), eq(5)))
                .thenReturn(new GroupPageResponse(List.of(group), "next-token"));

        mockMvc.perform(
                        get("/api/google/groups")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groups[0].name").value("g1"))
                .andExpect(jsonPath("$.nextPageToken").value("next-token"));
    }

    @Test
    void getGroups_passesQueryAndPageTokenAndSize() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(googleGroupsService.getGroupsPaged(
                        "admin@acme.com", "q", "pt", 20))
                .thenReturn(new GroupPageResponse(List.of(), null));

        mockMvc.perform(
                        get("/api/google/groups")
                                .contextPath("/api")
                                .param("query", "q")
                                .param("pageToken", "pt")
                                .param("size", "20")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(googleGroupsService)
                .getGroupsPaged("admin@acme.com", "q", "pt", 20);
    }

    @Test
    void getOverview_callsPreferencesAndReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(preferenceService.getDisabledPreferenceKeys("admin@acme.com"))
                .thenReturn(Set.of());
        var breakdown =
                new SecurityScoreBreakdownDto(
                        80,
                        "ok",
                        List.of(
                                new SecurityScoreFactorDto(
                                        "t", "d", 10, 20, "low")));
        var warnings = new SectionWarningsDto(Map.of(), false, false);
        when(googleGroupsService.getGroupsOverview("admin@acme.com", Set.of()))
                .thenReturn(
                        new GroupOverviewResponse(1, 0, 0, 0, 0, 80, breakdown, warnings));

        mockMvc.perform(
                        get("/api/google/groups/overview")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGroups").value(1))
                .andExpect(jsonPath("$.securityScore").value(80));

        verify(preferenceService).getDisabledPreferenceKeys("admin@acme.com");
    }

    @Test
    void postRefresh_validatesTokenAndRefreshesCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        mockMvc.perform(
                        post("/api/google/groups/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(googleGroupsService).forceRefreshCache("admin@acme.com");
    }
}
