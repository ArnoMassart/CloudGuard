package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.exception.InvalidExternalTokenException;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import com.cloudmen.cloudguard.unit.helper.JwtTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private JwtDecoder decoder;

    private ResourceBundleMessageSource messageSource;
    private JwtService service;

    private static final String SECRET = "a-very-secure-secret-key-that-is-at-least-32-bytes-long!";
    private static final long EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        service = new JwtService(messageSource);

        ReflectionTestUtils.setField(service, "googleJwtDecoder", decoder);
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "jwtExpiration", EXPIRATION);
        service.init();
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void decodeGoogleToken_validToken_returnsJwt() {
        Jwt mockJwt = mock(Jwt.class);

        when(decoder.decode("valid-token")).thenReturn(mockJwt);

        Jwt result = service.decodeGoogleToken("valid-token");

        assertEquals(mockJwt, result);
    }

    @Test
    void decodeGoogleToken_invalidToken_throwsInvalidExternalTokenException() {
        when(decoder.decode("invalid-token")).thenThrow(new org.springframework.security.oauth2.jwt.JwtException("Expired"));

        assertThrows(InvalidExternalTokenException.class, () -> service.decodeGoogleToken("invalid-token"));
    }

    @Test
    void generateToken_createsValidJwtAndCanBeValidated() {
        User user = JwtTestHelper.createUser(GlobalTestHelper.ADMIN);

        String token = service.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String subject = service.validateInternalToken(token);

        Assertions.assertEquals(GlobalTestHelper.ADMIN, subject);
    }

    @Test
    void validateInternalToken_invalidToken_throwsException() {
        assertThrows(UnauthorizedException.class, () -> service.validateInternalToken("invalid.internal.token.string"));
    }

    @Test
    void isGoogleAdmin_true() {
        Jwt jwt = JwtTestHelper.mockJwtWithAdminClaim(true);
        assertTrue(service.isGoogleAdmin(jwt));
    }

    @Test
    void isGoogleAdmin_false() {
        Jwt jwt = JwtTestHelper.mockJwtWithAdminClaim(false);
        assertFalse(service.isGoogleAdmin(jwt));
    }

    @Test
    void isGoogleAdmin_null() {
        Jwt jwt = JwtTestHelper.mockJwtWithAdminClaim(null);
        assertFalse(service.isGoogleAdmin(jwt));
    }
}