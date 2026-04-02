package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.AuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private GoogleUsersCacheService usersCacheService;

    private static final String EMAIL = "admin@cloudmen.com";
    private static final String EXTERNAL_TOKEN = "external-google-id-token";
    private static final String INTERNAL_TOKEN = "internal-session-token";

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

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(EMAIL);
        testUser.setPictureUrl("https://picture.url/old");
        testUser.setCreatedAt(LocalDateTime.now());

        testUserDto = new UserDto(
                EMAIL,
                "Test",
                "Admin",
                "https://picture.url/old",
                LocalDateTime.now()
        );
        testUserDto.setEmail(EMAIL);
    }

    @Test
    void processLogin_existingAdminUser_updatesPictureAndReturnsLoginResult() {
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        LoginResult result = authService.processLogin(EXTERNAL_TOKEN);

        assertNotNull(result);
        assertEquals(INTERNAL_TOKEN, result.cookie().getValue());
        assertEquals(testUserDto, result.userDto());
        verify(userService, times(1)).save(testUser);
        assertEquals("https://picture.url/new", testUser.getPictureUrl());
    }

    @Test
    void processLogin_newAdminUser_registersAndReturnsLoginResult() {
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userService.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn(INTERNAL_TOKEN);
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        LoginResult result = authService.processLogin(EXTERNAL_TOKEN);

        assertNotNull(result);
        assertEquals(INTERNAL_TOKEN, result.cookie().getValue());
        assertEquals(testUserDto, result.userDto());
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    void processLogin_nonAdminUser_throwsException() {
        when(jwtService.decodeGoogleToken(EXTERNAL_TOKEN)).thenReturn(mockJwt);
        when(jwtService.isGoogleAdmin(mockJwt)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.processLogin(EXTERNAL_TOKEN);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userService, never()).findByEmail(anyString());
    }

    @Test
    void validateSession_validToken_returnsUserDto() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);

        UserDto result = authService.validateSession(INTERNAL_TOKEN);

        assertNotNull(result);
        assertEquals(EMAIL, result.getEmail());
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

    @Test
    void getCurrentUser_validToken_returnsUserWithRoles() {
        when(jwtService.validateInternalToken(INTERNAL_TOKEN)).thenReturn(EMAIL);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userService.convertToDto(testUser)).thenReturn(testUserDto);
        when(usersCacheService.getUserRoles(EMAIL)).thenReturn(List.of("_SEED_ADMIN_ROLE", "_READ_ONLY_ADMIN_ROLE"));

        Optional<UserDto> result = authService.getCurrentUser(INTERNAL_TOKEN);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getRoles());
        assertEquals(2, result.get().getRoles().size());
        assertTrue(result.get().getRoles().contains("Super Admin"));
        assertTrue(result.get().getRoles().contains("Read Only Admin"));
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

    @Test
    void translateRoleName_returnsCorrectTranslations() {
        assertEquals("Super Admin", authService.translateRoleName("_SEED_ADMIN_ROLE"));
        assertEquals("Read Only Admin", authService.translateRoleName("_READ_ONLY_ADMIN_ROLE"));
        assertEquals("Admin", authService.translateRoleName("SOMETHING_ELSE"));
    }
}
