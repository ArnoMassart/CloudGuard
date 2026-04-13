package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.NotificationController;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.dto.notifications.NotificationsResponse;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
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
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
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
    NotificationControllerIT.MessageSourceTestConfig.class
})
class NotificationControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationAggregationService aggregationService;

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

    private static NotificationDto sampleNotification() {
        return new NotificationDto(
                "n1",
                "HIGH",
                "Title",
                "Description",
                List.of("act1"),
                "dns-critical",
                "domain-dns",
                "DNS",
                "/domain-dns",
                false,
                false,
                true);
    }

    @Test
    void getNotifications_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getNotifications_withCookie_returnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        var response =
                new NotificationsResponse(List.of(sampleNotification()), List.of());
        when(aggregationService.getNotifications(eq("admin@acme.com"), any(Locale.class)))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/notifications")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active[0].id").value("n1"))
                .andExpect(jsonPath("$.active[0].source").value("domain-dns"))
                .andExpect(jsonPath("$.dismissed").isArray());

        verify(aggregationService).getNotifications(eq("admin@acme.com"), any(Locale.class));
    }
}
