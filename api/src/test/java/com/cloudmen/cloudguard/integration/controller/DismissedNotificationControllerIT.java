package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.DismissedNotificationController;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.DismissedNotificationService;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DismissedNotificationController.class,
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
    DismissedNotificationControllerIT.MessageSourceTestConfig.class
})
class DismissedNotificationControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DismissedNotificationService dismissedNotificationService;

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

    @Test
    void markAsDismissed_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/notifications/dismissed")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markAsDismissed_withCookie_returns200() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        post("/api/notifications/dismissed")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"source\":\"domain-dns\",\"notificationType\":\"dns-critical\","
                                                + "\"sourceLabel\":\"DNS\",\"sourceRoute\":\"/domain-dns\","
                                                + "\"title\":\"t\",\"description\":\"d\",\"severity\":\"HIGH\","
                                                + "\"recommendedActions\":[\"a\"]}")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(dismissedNotificationService)
                .markAsDismissed(
                        eq("user-1"),
                        argThat(
                                req ->
                                        "domain-dns".equals(req.source())
                                                && "dns-critical".equals(req.notificationType())));
    }

    @Test
    void unDismiss_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        delete("/api/notifications/dismissed")
                                .contextPath("/api")
                                .param("source", "a")
                                .param("notificationType", "b"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unDismiss_removed_returns200() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(dismissedNotificationService.unDismiss("user-1", "domain-dns", "dns-critical"))
                .thenReturn(true);

        mockMvc.perform(
                        delete("/api/notifications/dismissed")
                                .contextPath("/api")
                                .param("source", "domain-dns")
                                .param("notificationType", "dns-critical")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(dismissedNotificationService)
                .unDismiss("user-1", "domain-dns", "dns-critical");
    }

    @Test
    void unDismiss_notFound_returns404() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(dismissedNotificationService.unDismiss("user-1", "x", "y")).thenReturn(false);

        mockMvc.perform(
                        delete("/api/notifications/dismissed")
                                .contextPath("/api")
                                .param("source", "x")
                                .param("notificationType", "y")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isNotFound());
    }
}
