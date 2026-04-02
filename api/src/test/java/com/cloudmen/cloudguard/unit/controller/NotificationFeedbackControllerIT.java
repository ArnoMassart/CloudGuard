package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.NotificationFeedbackController;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationFeedbackService;
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
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationFeedbackController.class,
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
    NotificationFeedbackControllerIT.MessageSourceTestConfig.class
})
class NotificationFeedbackControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationFeedbackService notificationFeedbackService;

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
    void getFeedbackKeys_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications/feedback/keys").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getFeedbackKeys_withCookie_returnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(notificationFeedbackService.getFeedbackKeysForUser("user-1"))
                .thenReturn(Set.of("users-groups:alert"));

        mockMvc.perform(
                        get("/api/notifications/feedback/keys")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("users-groups:alert"));
    }

    @Test
    void hasFeedback_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/notifications/feedback")
                                .contextPath("/api")
                                .param("source", "s")
                                .param("notificationType", "t"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hasFeedback_withCookie_returnsBoolean() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(notificationFeedbackService.hasFeedback("user-1", "domain-dns", "dns-critical"))
                .thenReturn(true);

        mockMvc.perform(
                        get("/api/notifications/feedback")
                                .contextPath("/api")
                                .param("source", "domain-dns")
                                .param("notificationType", "dns-critical")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void submitFeedback_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/notifications/feedback")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"source\":\"a\",\"notificationType\":\"b\",\"feedbackText\":\"c\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitFeedback_withCookie_returns200AndCallsService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        post("/api/notifications/feedback")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"source":"users-groups","notificationType":"user-control","feedbackText":"thanks"}
                                        """)
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(notificationFeedbackService)
                .submitFeedback("user-1", "users-groups", "user-control", "thanks");
    }
}
