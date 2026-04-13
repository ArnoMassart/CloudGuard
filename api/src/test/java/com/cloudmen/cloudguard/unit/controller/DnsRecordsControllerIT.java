package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.DnsRecordsController;
import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP integration tests for {@link DnsRecordsController} (domain-dns page DNS API). */
@WebMvcTest(
        controllers = DnsRecordsController.class,
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
    DnsRecordsControllerIT.MessageSourceTestConfig.class
})
class DnsRecordsControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DnsRecordsService dnsRecordsService;

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

    private static DnsRecordResponseDto sampleResponse(String domain) {
        var row =
                new DnsRecordDto(
                        "SPF",
                        "@",
                        List.of("v=spf1 include:_spf.google.com ~all"),
                        DnsRecordStatus.VALID,
                        DnsRecordImportance.REQUIRED,
                        "ok");
        var breakdown =
                new SecurityScoreBreakdownDto(
                        90,
                        "ok",
                        List.of(
                                new SecurityScoreFactorDto(
                                        "SPF", "d", 10, 10, "low")));
        return new DnsRecordResponseDto(domain, List.of(row), 90, breakdown);
    }

    @Test
    void records_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        get("/api/google/dns-records/records")
                                .contextPath("/api")
                                .param("domain", "acme.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void records_withCookie_usesDefaultDkimSelector_andReturnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        Map<String, DnsRecordImportance> overrides = Map.of();
        when(preferenceService.getDnsImportanceOverrides("admin@acme.com")).thenReturn(overrides);
        when(dnsRecordsService.getImportantRecords("acme.com", "google", overrides))
                .thenReturn(sampleResponse("acme.com"));

        mockMvc.perform(
                        get("/api/google/dns-records/records")
                                .contextPath("/api")
                                .param("domain", "acme.com")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.domain").value("acme.com"))
                .andExpect(jsonPath("$.securityScore").value(90))
                .andExpect(jsonPath("$.rows[0].type").value("SPF"));

        verify(preferenceService).getDnsImportanceOverrides("admin@acme.com");
        verify(dnsRecordsService).getImportantRecords("acme.com", "google", overrides);
    }

    @Test
    void records_withCustomDkimSelector_passesToService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("admin@acme.com");
        Map<String, DnsRecordImportance> overrides = Map.of("SPF", DnsRecordImportance.OPTIONAL);
        when(preferenceService.getDnsImportanceOverrides("admin@acme.com")).thenReturn(overrides);
        when(dnsRecordsService.getImportantRecords("acme.com", "custom", overrides))
                .thenReturn(sampleResponse("acme.com"));

        mockMvc.perform(
                        get("/api/google/dns-records/records")
                                .contextPath("/api")
                                .param("domain", "acme.com")
                                .param("dkimSelector", "custom")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(dnsRecordsService).getImportantRecords("acme.com", "custom", overrides);
    }
}
