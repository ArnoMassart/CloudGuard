package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.GoogleDomainController;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.GoogleDomainService;
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
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP integration tests for {@link GoogleDomainController}. */
@WebMvcTest(
        controllers = GoogleDomainController.class,
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
    GoogleDomainControllerIT.MessageSourceTestConfig.class
})
class GoogleDomainControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleDomainService googleDomainService;

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
    void getAllDomains_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/google/domains").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getAllDomains_withCookie_returnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        var dto =
                new DomainDto("acme.com", "Primary Domain", true, 50);
        when(googleDomainService.getAllDomains("admin@acme.com")).thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/google/domains")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].domainName").value("acme.com"))
                .andExpect(jsonPath("$[0].domainType").value("Primary Domain"))
                .andExpect(jsonPath("$[0].isVerified").value(true))
                .andExpect(jsonPath("$[0].totalUsers").value(50));
    }

    @Test
    void postRefresh_validatesTokenAndRefreshesCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        mockMvc.perform(
                        post("/api/google/domains/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(googleDomainService).forceRefreshCache("admin@acme.com");
    }
}
