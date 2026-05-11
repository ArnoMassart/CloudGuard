package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.ContactController;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.ContactEmailService;
import com.cloudmen.cloudguard.service.JwtService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP slice test for {@link ContactController}: auth cookie, JSON binding, delegation to
 * {@link ContactEmailService}.
 */
@WebMvcTest(
        controllers = ContactController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.port=8080",
                "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        })
@Import({
        SecurityConfig.class,
        WebConfig.class,
        AuthInterceptor.class,
        GlobalExceptionHandler.class,
        ContactControllerIT.MessageSourceTestConfig.class
})
class ContactControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "user@test.local";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ContactEmailService contactEmailService;

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
    void sendContact_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        post("/api/contact/send")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"topic":"support","subject":"Hello","message":"This is long enough."}
                                        """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(contactEmailService);
    }

    @Test
    void sendContact_withValidCookie_callsEmailServiceAndReturns200() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        mockMvc.perform(
                        post("/api/contact/send")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"topic":"feedback","subject":"Subject line","message":"Message body with ten chars min"}
                                        """))
                .andExpect(status().isOk());

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(contactEmailService)
                .sendContactEmail(
                        eq(EMAIL),
                        eq("feedback"),
                        eq("Subject line"),
                        eq("Message body with ten chars min"));
    }
}
