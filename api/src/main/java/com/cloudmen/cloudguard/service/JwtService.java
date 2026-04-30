package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.exception.InvalidExternalTokenException;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * A core security service responsible for handling JSON Web Tokens (JWT). <p>
 *
 * This service serves a dual purpose: it validates and decodes external identity tokens (from the configured
 * Identity Provider, e.g., Auth0/Google), and it generates and verifies the internal application session tokens used
 * throughout the CloudGuard platform.
 */
@Service
public class JwtService {
    private static final String IDP_JWK_SET_URI = "https://dev-x2l40e775g2q2ot3.eu.auth0.com/.well-known/jwks.json";

    private final JwtDecoder externalJwtDecoder;
    private final MessageSource messageSource;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey signInKey;

    public JwtService(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
        this.externalJwtDecoder = NimbusJwtDecoder.withJwkSetUri(IDP_JWK_SET_URI).build();
    }

    @PostConstruct
    public void init() {
        this.signInKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Decodes and cryptographically validates an external Identity Provider token.
     *
     * @param externalIdToken the raw JWT string provided by the frontend authentication flow
     * @return a parsed and verified {@link Jwt} object containing the user's claims
     * @throws InvalidExternalTokenException if the token signature is invalid, missing, or expired
     */
    public Jwt decodeExternalToken(String externalIdToken) {
        try {
            return externalJwtDecoder.decode(externalIdToken);
        } catch (Exception e) {
            throw new InvalidExternalTokenException("Invalid External Token: " + e.getMessage());
        }
    }

    /**
     * Generates a new, time-limited internal session token for an authenticated user.
     *
     * @param user the domain user object for whom the token is being generated
     * @return a cryptographically signed JWT string representing the internal session
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(signInKey)
                .compact();
    }

    /**
     * Validates an internal session token and extracts the subject (user email).
     *
     * @param token the internal JWT string to validate
     * @return the user's email address extracted from the token's subject claim
     * @throws UnauthorizedException if the token is null, expired, or cryptographically tampered with
     */
    public String validateInternalToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException(
                    messageSource.getMessage("api.auth.token_required", null, LocaleContextHolder.getLocale()));
        }
        try {
            return Jwts.parser()
                    .verifyWith(signInKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(
                    messageSource.getMessage("api.auth.session_expired", null, LocaleContextHolder.getLocale()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(
                    messageSource.getMessage("api.auth.session_invalid", null, LocaleContextHolder.getLocale()));
        }
    }
}
