package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.OrganizationController;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.dto.organization.DatabaseOrgResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrganizationController.class,
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
        OrganizationControllerIT.MessageSourceTestConfig.class
})
public class OrganizationControllerIT {
    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String EMAIL = "admin@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private OrganizationService organizationService;

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
    void getAll_withoutCookie_returns401() throws Exception {
        // Vertel de mock: "Als je null krijgt, gooi dan een UnauthorizedException!"
        when(jwtService.validateInternalToken(isNull())).thenThrow(new UnauthorizedException("Token is missing"));

        mockMvc.perform(
                        get("/api/org/all")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_withValidCookie_returnsListJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        Organization org = new Organization();
        org.setName("Acme Corp");
        when(organizationService.getAll()).thenReturn(List.of(org));

        mockMvc.perform(
                        get("/api/org/all")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Acme Corp"));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(organizationService).getAll();
    }

    @Test
    void getAllPaged_withoutCookie_returns401() throws Exception {
        // Zelfde hier voor de paged variant
        when(jwtService.validateInternalToken(isNull())).thenThrow(new UnauthorizedException("Token is missing"));

        mockMvc.perform(
                        get("/api/org/all-paged")
                                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllPaged_withValidCookie_returnsPageJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        Organization org = new Organization();
        org.setName("Test Org");
        DatabaseOrgResponse mockResponse = new DatabaseOrgResponse(List.of(org), "next-page");

        // Standaard 'size' is 4 in de controller als deze niet wordt meegegeven
        when(organizationService.getAllPaged(isNull(), eq(4), isNull()))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/org/all-paged")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nextPageToken").value("next-page"))
                .andExpect(jsonPath("$.organizations[0].name").value("Test Org"));

        verify(jwtService).validateInternalToken(VALID_TOKEN);
        verify(organizationService).getAllPaged(null, 4, null);
    }

    @Test
    void getAllPaged_passesAllParamsToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(EMAIL);

        DatabaseOrgResponse mockResponse = new DatabaseOrgResponse(List.of(), null);
        when(organizationService.getAllPaged("pt", 20, "searchQuery"))
                .thenReturn(mockResponse);

        mockMvc.perform(
                        get("/api/org/all-paged")
                                .contextPath("/api")
                                .param("pageToken", "pt")
                                .param("size", "20")
                                .param("query", "searchQuery")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(organizationService).getAllPaged("pt", 20, "searchQuery");
    }
}
