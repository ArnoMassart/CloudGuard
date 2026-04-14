package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.AuthController;
import com.cloudmen.cloudguard.controller.TeamleaderAccessController;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
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
import java.util.Map;
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
        controllers = TeamleaderAccessController.class,
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
        TeamleaderAccessControllerIT.MessageSourceTestConfig.class
})
public class TeamleaderAccessControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamleaderAccessService teamleaderAccessService;

    @MockitoBean
    private JwtService jwtService;

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
    // POST SETUP TESTS
    // =========================================================================================

    @Test
    void setupTeamLeader_withValidTokens_returns200() throws Exception {
        Map<String, String> tokens = Map.of(
                "access_token", "valid-access",
                "refresh_token", "valid-refresh"
        );

        // Bij void methodes die succesvol moeten uitvoeren hoeven we niets specifieks te mocken

        mockMvc.perform(
                        post("/api/teamleader/setup")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(tokens)))
                .andExpect(status().isOk())
                .andExpect(content().string("Teamleader credentials succesvol geconfigureerd via de service."));

        verify(teamleaderAccessService).updateCredentials("valid-access", "valid-refresh");
    }

    @Test
    void setupTeamLeader_serviceThrowsIllegalArgumentException_returns400() throws Exception {
        Map<String, String> tokens = Map.of("access_token", "invalid");

        doThrow(new IllegalArgumentException("Ongeldige tokens opgegeven"))
                .when(teamleaderAccessService).updateCredentials("invalid", null);

        mockMvc.perform(
                        post("/api/teamleader/setup")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(tokens)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ongeldige tokens opgegeven"));
    }

    @Test
    void setupTeamLeader_serviceThrowsGeneralException_returns500() throws Exception {
        Map<String, String> tokens = Map.of(
                "access_token", "access",
                "refresh_token", "refresh"
        );

        doThrow(new RuntimeException("Database offline"))
                .when(teamleaderAccessService).updateCredentials("access", "refresh");

        mockMvc.perform(
                        post("/api/teamleader/setup")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(tokens)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Er is een onverwachte fout opgetreden."));
    }

    // =========================================================================================
    // GET CHECK TESTS
    // =========================================================================================

    @Test
    void checkAccess_withoutCookie_returns401() throws Exception {
        // Zonder cookie mag het request niet lukken (afhankelijk van je interceptor / jwt validation config)
        mockMvc.perform(
                        get("/api/teamleader/check")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkAccess_withValidCookie_userHasAccess_returnsTrue() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(teamleaderAccessService.hasCloudGuardAccess(EMAIL)).thenReturn(true);

        mockMvc.perform(
                        get("/api/teamleader/check")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasAccess").value(true));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(teamleaderAccessService).hasCloudGuardAccess(EMAIL);
    }

    @Test
    void checkAccess_withValidCookie_userNoAccess_returnsFalse() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);
        when(teamleaderAccessService.hasCloudGuardAccess(EMAIL)).thenReturn(false);

        mockMvc.perform(
                        get("/api/teamleader/check")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasAccess").value(false));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(teamleaderAccessService).hasCloudGuardAccess(EMAIL);
    }
}
