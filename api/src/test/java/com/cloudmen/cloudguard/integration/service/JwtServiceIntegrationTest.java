package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.exception.InvalidExternalTokenException;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import com.cloudmen.cloudguard.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "application.security.jwt.secret-key=thisisaverylongsecretkeythatisatleast32byteslong",
        "application.security.jwt.expiration=3600000"
})
public class JwtServiceIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @MockitoBean(name = "messageSource")
    private MessageSource messageSource;

    private JwtDecoder jwtDecoder;

    private static final String SECRET = "thisisaverylongsecretkeythatisatleast32byteslong";

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        ReflectionTestUtils.setField(jwtService, "googleJwtDecoder", jwtDecoder);

        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void decodeGoogleToken_validToken_returnsJwt() {
        Jwt expectedJwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid-token")).thenReturn(expectedJwt);

        Jwt result = jwtService.decodeGoogleToken("valid-token");

        assertNotNull(result);
        assertEquals(expectedJwt, result);
    }

    @Test
    void decodeGoogleToken_invalidToken_throwsException() {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("Invalid"));

        assertThrows(InvalidExternalTokenException.class, () -> {
            jwtService.decodeGoogleToken("invalid-token");
        });
    }

    @Test
    void generateToken_createsValidJwtString() {
        User user = new User();
        user.setEmail("admin@cloudmen.com");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void validateInternalToken_validToken_returnsSubject() {
        User user = new User();
        user.setEmail("admin@cloudmen.com");
        String token = jwtService.generateToken(user);

        String subject = jwtService.validateInternalToken(token);

        assertEquals("admin@cloudmen.com", subject);
    }

    @Test
    void validateInternalToken_emptyToken_throwsException() {
        assertThrows(UnauthorizedException.class, () -> {
            jwtService.validateInternalToken(null);
        });

        assertThrows(UnauthorizedException.class, () -> {
            jwtService.validateInternalToken("");
        });
    }

    @Test
    void validateInternalToken_invalidSignature_throwsException() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalid_signature_here";

        assertThrows(UnauthorizedException.class, () -> {
            jwtService.validateInternalToken(invalidToken);
        });
    }

    @Test
    void validateInternalToken_expiredToken_throwsException() {
        String expiredToken = Jwts.builder()
                .subject("admin@cloudmen.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertThrows(UnauthorizedException.class, () -> {
            jwtService.validateInternalToken(expiredToken);
        });
    }

    @Test
    void isGoogleAdmin_claimTrue_returnsTrue() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaim("https://cloudguard.com/is_admin")).thenReturn(true);

        assertTrue(jwtService.isGoogleAdmin(mockJwt));
    }

    @Test
    void isGoogleAdmin_claimFalse_returnsFalse() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaim("https://cloudguard.com/is_admin")).thenReturn(false);

        assertFalse(jwtService.isGoogleAdmin(mockJwt));
    }

    @Test
    void isGoogleAdmin_claimNull_returnsFalse() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaim("https://cloudguard.com/is_admin")).thenReturn(null);

        assertFalse(jwtService.isGoogleAdmin(mockJwt));
    }
}
