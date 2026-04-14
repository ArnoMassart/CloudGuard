package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.WorkspaceCustomerIdResolver;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.unit.helper.AuthTestHelper;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private GoogleUsersCacheService usersCacheService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private WorkspaceCustomerIdResolver workspaceCustomerIdResolver;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userService,
                jwtService,
                userRepository,
                usersCacheService,
                organizationService,
                workspaceCustomerIdResolver);
        lenient().when(workspaceCustomerIdResolver.resolveWorkspaceCustomer(any())).thenReturn(Optional.empty());
    }

    @Test
    void processLogin_nonAdminUser_throwsForbiddenException() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                authService.processLogin("ext-token")
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void processLogin_existingUserWithSamePicture_returnsLoginResultWithoutSaving() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        LoginResult result = authService.processLogin("ext-token");

        assertNotNull(result);
        assertEquals("internal-token", result.cookie().getValue());
        assertEquals(dto, result.userDto());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    void processLogin_existingUserWithDifferentPicture_updatesPictureAndSaves() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "new-pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "old-pic.jpg");
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("internal-token");
        when(userService.convertToDto(existingUser)).thenReturn(dto);

        authService.processLogin("ext-token");

        assertEquals("new-pic.jpg", existingUser.getPictureUrl());
        verify(userService).save(existingUser);
        verify(organizationService).ensureUserLinkedToOrganization(eq(existingUser), isNull(), isNull());
    }

    @Test
    void processLogin_newUser_registersAndSaves() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt("new@x.com", "Jane", "Smith", "pic.jpg");
        User savedUser = AuthTestHelper.createDbUser("new@x.com", "pic.jpg");
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail("new@x.com")).thenReturn(Optional.empty());
        when(userService.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("internal-token");
        when(userService.convertToDto(savedUser)).thenReturn(dto);

        authService.processLogin("ext-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("new@x.com", capturedUser.getEmail());
        assertEquals("Jane", capturedUser.getFirstName());
        assertEquals("Smith", capturedUser.getLastName());
        assertEquals("pic.jpg", capturedUser.getPictureUrl());
        assertNotNull(capturedUser.getCreatedAt());
        verify(organizationService).ensureUserLinkedToOrganization(eq(savedUser), isNull(), isNull());
    }

    @Test
    void processLogin_resolvedWorkspaceCustomer_passesIdAndDisplayName() {
        Jwt mockJwt = AuthTestHelper.mockGoogleJwt(GlobalTestHelper.ADMIN, "John", "Doe", "pic.jpg");
        User existingUser = AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "pic.jpg");
        UserDto dto = AuthTestHelper.mockUserDto();

        when(jwtService.decodeGoogleToken("ext-token")).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(existingUser));
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
    void getCurrentUser_validToken_returnsUserDtoWithTranslatedRoles() {
        UserDto dto = AuthTestHelper.mockUserDto();
        when(jwtService.validateInternalToken("token")).thenReturn(GlobalTestHelper.ADMIN);
        when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(AuthTestHelper.createDbUser(GlobalTestHelper.ADMIN, "")));
        when(userService.convertToDto(any())).thenReturn(dto);
        when(usersCacheService.getUserRoles(GlobalTestHelper.ADMIN)).thenReturn(List.of("_SEED_ADMIN_ROLE", "UNKNOWN_ROLE"));

        Optional<UserDto> result = authService.getCurrentUser("token");

        assertTrue(result.isPresent());
        verify(dto).setRoles(List.of("Super Admin", "Admin"));
    }

    @Test
    void getCurrentUser_exceptionThrown_returnsEmptyOptional() {
        when(jwtService.validateInternalToken("bad-token")).thenThrow(new RuntimeException("Error"));

        Optional<UserDto> result = authService.getCurrentUser("bad-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void translateRoleName_mapsRolesCorrectly() {
        assertEquals("Super Admin", authService.translateRoleName("_SEED_ADMIN_ROLE"));
        assertEquals("Read Only Admin", authService.translateRoleName("_READ_ONLY_ADMIN_ROLE"));
        assertEquals("Admin", authService.translateRoleName("ANY_OTHER_ROLE"));
    }
}
