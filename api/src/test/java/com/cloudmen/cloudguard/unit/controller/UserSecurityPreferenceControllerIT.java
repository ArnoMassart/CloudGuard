package com.cloudmen.cloudguard.unit.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.UserSecurityPreferenceController;
import com.cloudmen.cloudguard.dto.preferences.PreferencesResponse;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PasswordSettingsService;
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
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserSecurityPreferenceController.class,
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
    UserSecurityPreferenceControllerIT.MessageSourceTestConfig.class
})
class UserSecurityPreferenceControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSecurityPreferenceService preferenceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PasswordSettingsService passwordSettingsService;

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
    void getAllPreferences_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/user/preferences").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void getAllPreferences_withCookie_returnsJson() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        var prefs =
                new PreferencesResponse(
                        Map.of("domain-dns:toggle", true),
                        Map.of("SPF", "REQUIRED"),
                        Set.of("SPF"));
        when(preferenceService.getPreferencesResponse("user-1")).thenReturn(prefs);

        mockMvc.perform(
                        get("/api/user/preferences")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.preferences['domain-dns:toggle']").value(true))
                .andExpect(jsonPath("$.dnsImportance.SPF").value("REQUIRED"));
    }

    @Test
    void getSectionPreferences_returnsMap() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(preferenceService.getPreferencesForSection("user-1", "domain-dns"))
                .thenReturn(Map.of("impSpf", true));

        mockMvc.perform(
                        get("/api/user/preferences/domain-dns")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impSpf").value(true));
    }

    @Test
    void getDisabledKeys_returnsSortedList() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");
        when(preferenceService.getDisabledPreferenceKeys("user-1"))
                .thenReturn(Set.of("z:1", "a:2", "m:3"));

        mockMvc.perform(
                        get("/api/user/preferences/disabled")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("a:2"))
                .andExpect(jsonPath("$[1]").value("m:3"))
                .andExpect(jsonPath("$[2]").value("z:1"));
        
        verify(preferenceService).getDisabledPreferenceKeys("user-1");
    }

    @Test
    void setPreference_domainDns_doesNotRefreshPasswordCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        put("/api/user/preferences")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"section\":\"domain-dns\",\"preferenceKey\":\"impSpf\","
                                                + "\"enabled\":true,\"value\":\"OPTIONAL\"}")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService)
                .setPreference("user-1", "domain-dns", "impSpf", true, "OPTIONAL");
        verifyNoInteractions(passwordSettingsService);
    }

    @Test
    void setPreference_passwordSettings_refreshesPasswordCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        put("/api/user/preferences")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"section\":\"password-settings\",\"preferenceKey\":\"warnKeys\","
                                                + "\"enabled\":false,\"value\":\"\"}")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService)
                .setPreference("user-1", "password-settings", "warnKeys", false, "");
        verify(passwordSettingsService).forceRefreshCache("user-1");
    }

    @Test
    void setSectionPreferences_passwordSettings_refreshesPasswordCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        put("/api/user/preferences/section")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"section\":\"password-settings\",\"preferences\":{\"k\":true}}")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService)
                .setSectionPreferences(
                        eq("user-1"),
                        eq("password-settings"),
                        argThat(m -> m.size() == 1 && Boolean.TRUE.equals(m.get("k"))));
        verify(passwordSettingsService).forceRefreshCache("user-1");
    }

    @Test
    void setSectionPreferences_otherSection_doesNotRefreshPasswordCache() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn("user-1");

        mockMvc.perform(
                        put("/api/user/preferences/section")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"section\":\"domain-dns\",\"preferences\":{\"impMx\":false}}")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(preferenceService)
                .setSectionPreferences(
                        eq("user-1"),
                        eq("domain-dns"),
                        argThat(m -> m.size() == 1 && Boolean.FALSE.equals(m.get("impMx"))));
        verifyNoInteractions(passwordSettingsService);
    }

    @Test
    void setPreference_withoutCookie_returns401() throws Exception {
        mockMvc.perform(
                        put("/api/user/preferences")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"section\":\"domain-dns\",\"preferenceKey\":\"k\",\"enabled\":true}")
                )
                .andExpect(status().isUnauthorized());
    }
}
