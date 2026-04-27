package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.WorkspaceCustomerIdResolver;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.unit.helper.AuthTestHelper;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cloudmen.cloudguard.unit.helper.AuthTestHelper.mockDwdCheck;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private WorkspaceCustomerIdResolver workspaceCustomerIdResolver;

    @Mock
    private GoogleApiFactory googleApiFactory;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userService,
                jwtService,
                userRepository,
                organizationService,
                workspaceCustomerIdResolver,
                googleApiFactory);

        // Standaard geen customer resolven tenzij specifiek nodig in de test
        lenient().when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void processLogin_nonAdminUser_assignsUnassignedRole() throws Exception {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        existingUser.setRoles(new ArrayList<>());
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        // We triggeren de DWD check flow, maar simuleren dat Google zegt "isAdmin = false"
        existingUser.setOrganizationId(1L);
        Organization mockOrg = mock(Organization.class);
        when(mockOrg.getAdminEmail()).thenReturn("admin@org.com");

        when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(GlobalTestHelper.ADMIN))
                .thenReturn(Optional.of(new WorkspaceCustomer("C-123", "Acme")));
        when(organizationService.findById(1L)).thenReturn(Optional.of(mockOrg));
        mockDwdCheck(GlobalTestHelper.ADMIN, "admin@org.com", false, googleApiFactory);

        authService.processLogin("ext-token");

        // Role moet fallbacken naar UNASSIGNED, omdat de check faalde of false teruggaf
        assertTrue(existingUser.getRoles().contains(UserRole.UNASSIGNED));
        verify(userService, atLeastOnce()).save(existingUser);
    }

    @Test
    void processLogin_adminUser_assignsSuperAdminRole() throws Exception {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        existingUser.setRoles(new ArrayList<>(List.of(UserRole.UNASSIGNED)));
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        // Mock organisatie en geslaagde DWD check
        existingUser.setOrganizationId(2L);
        Organization mockOrg = mock(Organization.class);
        when(mockOrg.getAdminEmail()).thenReturn("admin@org.com");

        when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(GlobalTestHelper.ADMIN))
                .thenReturn(Optional.of(new WorkspaceCustomer("C-123", "Acme")));
        when(organizationService.findById(2L)).thenReturn(Optional.of(mockOrg));
        mockDwdCheck(GlobalTestHelper.ADMIN, "admin@org.com", true, googleApiFactory);

        authService.processLogin("ext-token");

        // User moet nu de SUPER_ADMIN rol hebben gekregen
        assertTrue(existingUser.getRoles().contains(UserRole.SUPER_ADMIN));
        assertFalse(existingUser.getRoles().contains(UserRole.UNASSIGNED));
        verify(userService, atLeastOnce()).save(existingUser);
    }

    @Test
    void processLogin_existingUserWithSamePictureAndCorrectRoles_returnsLoginResultWithoutSaving() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        existingUser.setRoles(new ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        // Geen organization resolves -> DWD check wordt overgeslagen
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        LoginResult result = authService.processLogin("ext-token");

        assertNotNull(result);
        assertEquals("internal-token", result.cookie().getValue());
        assertEquals(dto, result.userDto());

        // Geen wijzigingen, dus geen saves nodig
        verify(userService, never()).save(any(User.class));
    }

    @Test
    void processLogin_existingUserWithDifferentPicture_updatesPictureAndSaves() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "new-pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "old-pic.jpg");
        existingUser.setRoles(new ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        authService.processLogin("ext-token");

        assertEquals("new-pic.jpg", existingUser.getPictureUrl());
        verify(userService, times(1)).save(existingUser);
    }

    @Test
    void processLogin_newUser_registersAndSaves() throws Exception {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt("new@x.com", "Jane", "Smith", "pic.jpg");
        User savedUser = AuthTestHelper.createDbUser("new@x.com", "pic.jpg");
        savedUser.setRoles(new ArrayList<>());
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(userService.findByEmailOptional("new@x.com")).thenReturn(Optional.empty());
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("internal-token");
        when(userService.convertToDto(savedUser)).thenReturn(dto);

        authService.processLogin("ext-token");

        // 1 save voor registratie, 1 save voor het toewijzen van UNASSIGNED
        verify(userService, atLeast(2)).save(any(User.class));
    }

    @Test
    void processLogin_resolvedWorkspaceCustomer_passesIdAndDisplayName() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        existingUser.setRoles(new ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));

        when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(GlobalTestHelper.ADMIN))
                .thenReturn(Optional.of(new WorkspaceCustomer("C-123", "Acme Corp")));

        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        authService.processLogin("ext-token");

        verify(organizationService).ensureUserLinkedToOrganization(existingUser, "C-123", "Acme Corp");
    }

    @Test
    void validateSession_nullOrEmptyToken_returnsNull() {
        assertNull(authService.validateSession(null));
        assertNull(authService.validateSession(""));
    }

    @Test
    void validateSession_validToken_returnsUserDto() {
        User user = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.validateInternalToken("valid-token")).thenReturn(GlobalTestHelper.ADMIN);
        when(userRepository.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(user));
        when(userService.convertToDto(user)).thenReturn(dto);

        UserDto result = authService.validateSession("valid-token");

        assertEquals(dto, result);
    }

    @Test
    void validateSession_tokenValidButUserNotFound_returnsNull() {
        when(jwtService.validateInternalToken("valid-token")).thenReturn(GlobalTestHelper.ADMIN);
        when(userRepository.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.empty());

        assertNull(authService.validateSession("valid-token"));
    }

    @Test
    void validateSession_invalidTokenThrowsException_returnsNull() {
        when(jwtService.validateInternalToken("invalid-token")).thenThrow(new RuntimeException("Bad token"));

        assertNull(authService.validateSession("invalid-token"));
    }

    @Test
    void createEmptyCookie_returnsExpiredCookie() {
        ResponseCookie cookie = authService.createEmptyCookie();

        assertEquals("AuthToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void createSessionCookie_returnsValidSessionCookie() {
        ResponseCookie cookie = authService.createSessionCookie("my-token");

        assertEquals("AuthToken", cookie.getName());
        assertEquals("my-token", cookie.getValue());
        assertEquals(86400, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
    }

    @Test
    void getCurrentUser_validToken_returnsUserDto() {
        UserDto dto = AuthTestHelper.mockUserDto();
        when(jwtService.validateInternalToken("token")).thenReturn(GlobalTestHelper.ADMIN);
        when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "")));
        when(userService.convertToDto(any())).thenReturn(dto);

        Optional<UserDto> result = authService.getCurrentUser("token");

        assertTrue(result.isPresent());
        assertEquals(dto, result.get());
    }

    @Test
    void getCurrentUser_exceptionThrown_returnsEmptyOptional() {
        when(jwtService.validateInternalToken("bad-token")).thenThrow(new RuntimeException("Error"));

        Optional<UserDto> result = authService.getCurrentUser("bad-token");

        assertTrue(result.isEmpty());
    }
}