package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.security.WorkspaceIdentityClaims;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.WorkspaceCustomerIdResolver;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AuthService.class})
public class AuthServiceIT {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private WorkspaceCustomerIdResolver workspaceCustomerIdResolver;

    private static final String EMAIL = "admin@cloudmen.com";
    private static final String EXTERNAL_TOKEN = "external-google-id-token";
    private static final String INTERNAL_TOKEN = "internal-session-token";
    private static final String WORKSPACE_ID = "C0123456";
    private static final String WORKSPACE_NAME = "Test Workspace";

    private Jwt mockJwt;
    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        when(mockJwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(mockJwt.getClaimAsString("given_name")).thenReturn("Test");
        when(mockJwt.getClaimAsString("family_name")).thenReturn("Admin");
        when(mockJwt.getClaimAsString("picture")).thenReturn("https://picture.url/new");
        when(mockJwt.getClaimAsString(WorkspaceIdentityClaims.GOOGLE_WORKSPACE_CUSTOMER_ID)).thenReturn(WORKSPACE_ID);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(EMAIL);
        testUser.setPictureUrl("https://picture.url/old");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setRoles(new ArrayList<>(List.of(UserRole.UNASSIGNED)));

        testUserDto = new UserDto(
                EMAIL,
                "Test",
                "Admin",
                "https://picture.url/old",
                new ArrayList<>(),
                LocalDateTime.now(),
                false,
                false,
                0L,
                "Organisation"
        );

        // Default happy path mocks for dependencies injected via the new processLogin flow
        WorkspaceCustomer wsCustomer = new WorkspaceCustomer(WORKSPACE_ID, WORKSPACE_NAME);
        when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(EMAIL)).thenReturn(Optional.of(wsCustomer));
        doNothing().when(organizationService).ensureUserLinkedToOrganization(any(), anyString(), anyString());
    }

    // =========================================================================
    // processLogin() Tests
    // =========================================================================

    @Test
    void processLogin_existingUserIsGoogleSuperAdmin_upgradesToSuperAdminAndReturnsResult() {
        // Arrange
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true); // User IS Google Admin
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        // Act
        LoginResult result = authService.processLogin(EXTERNAL_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals(INTERNAL_TOKEN, result.cookie().getValue());
        verify(organizationService).ensureUserLinkedToOrganization(testUser, WORKSPACE_ID, WORKSPACE_NAME);

        // Assert syncSuperAdminStatus upgraded the user
        assertTrue(testUser.getRoles().contains(UserRole.SUPER_ADMIN));
        assertFalse(testUser.getRoles().contains(UserRole.UNASSIGNED));

        // Save is called twice: once for syncSuperAdminStatus, once for updating the pictureUrl
        verify(userService, times(2)).save(testUser);
        assertEquals("https://picture.url/new", testUser.getPictureUrl());
    }

    @Test
    void processLogin_existingUserIsNotGoogleAdmin_downgradesToUnassignedAndReturnsResult() {
        // Arrange
        testUser.setRoles(new ArrayList<>(List.of(UserRole.SUPER_ADMIN))); // Starts as Super Admin
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(false); // User IS NOT Google Admin
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        // Act
        authService.processLogin(EXTERNAL_TOKEN);

        // Assert
        assertTrue(testUser.getRoles().contains(UserRole.UNASSIGNED));
        assertFalse(testUser.getRoles().contains(UserRole.SUPER_ADMIN));
        verify(userService, atLeastOnce()).save(testUser);
    }

    @Test
    void processLogin_newUser_registersSetsUnassignedAndLinksOrg() {
        // Arrange
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty()); // New User
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(false);
        when(userService.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        // Act
        LoginResult result = authService.processLogin(EXTERNAL_TOKEN);

        // Assert
        assertNotNull(result);
        verify(userService, atLeastOnce()).save(any(User.class)); // 1x Register, 1x SyncRoles
        verify(organizationService).ensureUserLinkedToOrganization(testUser, WORKSPACE_ID, WORKSPACE_NAME);
        assertNull(testUser.getFirstName());
    }

    @Test
    void processLogin_workspaceCustomerNotResolved_fallsBackToJwtClaim() {
        // Arrange
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(false);
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        // Explicitly fail to resolve via API
        when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(EMAIL)).thenReturn(Optional.empty());

        // Act
        authService.processLogin(EXTERNAL_TOKEN);

        // Assert: It should fallback to the WORKSPACE_ID injected in the mockJwt setup, and displayName is null
        verify(organizationService).ensureUserLinkedToOrganization(testUser, WORKSPACE_ID, null);
    }

    // =========================================================================
    // validateSession() Tests
    // =========================================================================

    @Test
    void validateSession_validToken_returnsUserDto() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        UserDto result = authService.validateSession(INTERNAL_TOKEN);

        assertNotNull(result);
        assertEquals(EMAIL, result.email());
    }

    @Test
    void validateSession_emptyToken_returnsNull() {
        assertNull(authService.validateSession(null));
        assertNull(authService.validateSession(""));
    }

    @Test
    void validateSession_invalidToken_returnsNull() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        UserDto result = authService.validateSession(INTERNAL_TOKEN);

        assertNull(result);
    }

    // =========================================================================
    // Cookie Helper Tests
    // =========================================================================

    @Test
    void createEmptyCookie_returnsClearedCookie() {
        ResponseCookie cookie = authService.createEmptyCookie();

        assertEquals("AuthToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(0L, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void createSessionCookie_returnsConfiguredCookie() {
        ResponseCookie cookie = authService.createSessionCookie(INTERNAL_TOKEN);

        assertEquals("AuthToken", cookie.getName());
        assertEquals(INTERNAL_TOKEN, cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(86400L, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
    }

    // =========================================================================
    // getCurrentUser() Tests
    // =========================================================================

    @Test
    void getCurrentUser_validToken_returnsUserDto() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenReturn(EMAIL);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        Optional<UserDto> result = authService.getCurrentUser(INTERNAL_TOKEN);

        assertTrue(result.isPresent());
        assertEquals(EMAIL, result.get().email());
    }

    @Test
    void getCurrentUser_userNotFound_returnsEmpty() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenReturn(EMAIL);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Optional<UserDto> result = authService.getCurrentUser(INTERNAL_TOKEN);

        assertFalse(result.isPresent());
    }

    @Test
    void getCurrentUser_exceptionThrown_returnsEmpty() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenThrow(new RuntimeException("Error"));

        Optional<UserDto> result = authService.getCurrentUser(INTERNAL_TOKEN);

        assertFalse(result.isPresent());
    }

    // =========================================================================
    // translateRoleName() Tests
    // =========================================================================

    @Test
    void translateRoleName_returnsCorrectTranslations() {
        assertEquals("Super Admin", authService.translateRoleName("_SEED_ADMIN_ROLE"));
        assertEquals("Read Only Admin", authService.translateRoleName("_READ_ONLY_ADMIN_ROLE"));
        assertEquals("User", authService.translateRoleName("SOMETHING_ELSE"));
    }
}