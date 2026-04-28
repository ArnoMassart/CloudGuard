package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.configuration.SecurityConfig;
import com.cloudmen.cloudguard.configuration.WebConfig;
import com.cloudmen.cloudguard.controller.UserController;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserUpdateRoleRequest;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.exception.handler.GlobalExceptionHandler;
import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.Cookie;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for /user routes that use {@link CloudguardStaffService} (allowlist).
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@TestPropertySource(
        properties = {
                "server.port=8080", "server.servlet.context-path=/api",
                "spring.main.allow-bean-definition-overriding=true"
        })
@Import({
        SecurityConfig.class,
        WebConfig.class,
        AuthInterceptor.class,
        GlobalExceptionHandler.class,
        CloudguardStaffUserControllerIT.MessageSourceTestConfig.class
})
class CloudguardStaffUserControllerIT {

    private static final String AUTH_COOKIE = "AuthToken";
    private static final String VALID_TOKEN = "internal-jwt";
    private static final String STAFF_EMAIL = "staff@cloudmen.com";
    private static final String OTHER_EMAIL = "user@acme.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CloudguardStaffService cloudguardStaffService;

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

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(cloudguardStaffService).requireCloudmenAdmin(anyString());
    }

    // -------------------------------------------------------------------------
    // GET /user/is-cloudmen-staff
    // -------------------------------------------------------------------------

    @Test
    void isCloudmenStaff_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/user/is-cloudmen-staff").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isCloudmenStaff_withValidCookie_returnsBooleanFromService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(STAFF_EMAIL);
        when(cloudguardStaffService.isCloudmenAdmin(STAFF_EMAIL)).thenReturn(true);

        mockMvc.perform(
                        get("/api/user/is-cloudmen-staff")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(cloudguardStaffService).isCloudmenAdmin(STAFF_EMAIL);
    }

    @Test
    void isCloudmenStaff_withValidCookie_nonStaff_falseBody() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(OTHER_EMAIL);
        when(cloudguardStaffService.isCloudmenAdmin(OTHER_EMAIL)).thenReturn(false);

        mockMvc.perform(
                        get("/api/user/is-cloudmen-staff")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // -------------------------------------------------------------------------
    // GET /user/all (staff)
    // -------------------------------------------------------------------------

    @Test
    void getAllUsers_withStaffCookie_callsRequireAndService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(STAFF_EMAIL);
        DatabaseUsersResponse body = new DatabaseUsersResponse(List.of(), null);
        when(userService.getAll(isNull(), eq(4), isNull(), isNull())).thenReturn(body);

        mockMvc.perform(
                        get("/api/user/all")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray());

        verify(cloudguardStaffService).requireCloudmenAdmin(STAFF_EMAIL);
        verify(userService).getAll(null, 4, null, null);
    }

    @Test
    void getAllUsers_whenRequireThrows_forbidden() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(OTHER_EMAIL);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "denied"))
                .when(cloudguardStaffService).requireCloudmenAdmin(OTHER_EMAIL);

        mockMvc.perform(
                        get("/api/user/all")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isForbidden());

        verify(cloudguardStaffService).requireCloudmenAdmin(OTHER_EMAIL);
        verifyNoInteractions(userService);
    }

    // -------------------------------------------------------------------------
    // GET /user/all/requested-count
    // -------------------------------------------------------------------------

    @Test
    void getAllRequestedCount_withStaff_returnsCount() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(STAFF_EMAIL);
        when(userService.getAllRequestedCount()).thenReturn(7L);

        mockMvc.perform(
                        get("/api/user/all/requested-count")
                                .contextPath("/api")
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));

        verify(cloudguardStaffService).requireCloudmenAdmin(STAFF_EMAIL);
        verify(userService).getAllRequestedCount();
    }

    // -------------------------------------------------------------------------
    // POST /user/roles
    // -------------------------------------------------------------------------

    @Test
    void updateRoles_withStaff_delegatesToUserService() throws Exception {
        when(jwtService.validateInternalToken(VALID_TOKEN)).thenReturn(STAFF_EMAIL);
        UserUpdateRoleRequest req = new UserUpdateRoleRequest("target@x.com", List.of(UserRole.SUPER_ADMIN));

        mockMvc.perform(
                        post("/api/user/roles")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .cookie(new Cookie(AUTH_COOKIE, VALID_TOKEN)))
                .andExpect(status().isOk());

        verify(cloudguardStaffService).requireCloudmenAdmin(STAFF_EMAIL);
        verify(userService).updateRoles("target@x.com", List.of(UserRole.SUPER_ADMIN));
    }
}
