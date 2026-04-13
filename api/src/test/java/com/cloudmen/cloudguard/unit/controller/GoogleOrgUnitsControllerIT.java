package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.GoogleOrgUnitsController;
import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.GoogleOrgUnitService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.policy.OrgUnitPolicyAggregator;
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

/** HTTP integration tests: {@link AuthInterceptor}, controller wiring, and exception mapping. */
@WebMvcTest(
        controllers = GoogleOrgUnitsController.class,
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
    GoogleOrgUnitsControllerIT.MessageSourceTestConfig.class
})
class GoogleOrgUnitsControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOrgUnitService orgUnitService;

    @MockitoBean
    private OrgUnitPolicyAggregator orgUnitPolicyAggregator;

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
    void getOrgUnits_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/google/org-units").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getOrgUnits_withCookie_returnsTreeJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        var root = new OrgUnitNodeDto();
        root.setId("root-id");
        root.setName("acme.com");
        root.setOrgUnitPath("/");
        root.setUserCount(100);
        root.setRoot(true);
        root.setChildren(List.of());

        when(orgUnitService.getOrgUnitTree("admin@acme.com")).thenReturn(root);

        mockMvc.perform(
                        get("/api/google/org-units")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("acme.com"))
                .andExpect(jsonPath("$.orgUnitPath").value("/"))
                .andExpect(jsonPath("$.userCount").value(100))
                .andExpect(jsonPath("$.root").value(true));
    }

    @Test
    void getPolicies_defaultPath_callsAggregatorWithSlash() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        var row =
                new OrgUnitPolicyDto(
                        "key",
                        "title",
                        "desc",
                        "ok",
                        "ok-class",
                        "base",
                        "inh",
                        false,
                        "https://admin.test/",
                        "details");
        when(orgUnitPolicyAggregator.getPolicies("admin@acme.com", "/"))
                .thenReturn(List.of(row));

        mockMvc.perform(
                        get("/api/google/org-units/policies")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("key"));

        verify(orgUnitPolicyAggregator).getPolicies("admin@acme.com", "/");
    }

    @Test
    void getPolicies_withOrgUnitPath_passesParam() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        when(orgUnitPolicyAggregator.getPolicies("admin@acme.com", "/Sales/EMEA"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/google/org-units/policies")
                                .contextPath("/api")
                                .param("orgUnitPath", "/Sales/EMEA")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(orgUnitPolicyAggregator).getPolicies("admin@acme.com", "/Sales/EMEA");
    }

    @Test
    void postRefresh_validatesTokenAndRefreshesCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");

        mockMvc.perform(
                        post("/api/google/org-units/refresh")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache is succesvol vernieuwd!"));

        verify(orgUnitService).forceRefreshCache("admin@acme.com");
    }
}
