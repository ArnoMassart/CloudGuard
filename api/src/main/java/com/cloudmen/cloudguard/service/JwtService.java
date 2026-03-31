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

@Service
public class JwtService {
    private static final String GOOGLE_JWK_SET_URI = "https://dev-x2l40e775g2q2ot3.eu.auth0.com/.well-known/jwks.json";

    private final JwtDecoder googleJwtDecoder;
    private final MessageSource messageSource;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey signInKey;

    public JwtService(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
        this.googleJwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
    }

    @PostConstruct
    public void init() {
        this.signInKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public Jwt decodeGoogleToken(String externalIdToken) {
        try {
            return googleJwtDecoder.decode(externalIdToken);
        } catch (Exception e) {
            throw new InvalidExternalTokenException("Invalid External Token: " + e.getMessage());
        }
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(signInKey)
                .compact();
    }

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

    public boolean isGoogleAdmin(Jwt jwt) {
        Boolean isAdmin = jwt.getClaim("https://cloudguard.com/is_admin");
        return Boolean.TRUE.equals(isAdmin);
    }
}
