package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.UserDto;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    public LoginResult processLogin(String externalIdToken) {
        // 1. Decode the token to get all details (Name, Picture, Email)
        Jwt jwt = jwtService.decodeGoogleToken(externalIdToken);

        String email = jwt.getClaimAsString("email");

        // 2. Try to find user, OR create a new one if missing
        User user = userService.findByEmail(email)
                .orElseGet(() -> registerNewUser(jwt)); // <--- The Magic Logic

        // 3. Generate Session Token (Same as before)
        String sessionToken = jwtService.generateToken(user);
        ResponseCookie cookie = createSessionCookie(sessionToken);

        // 4. Return result
        UserDto userDto = userService.convertToDto(user);

        return new LoginResult(cookie, userDto);
    }

    private User registerNewUser(Jwt jwt) {
        User newUser = new User();

        newUser.setEmail(jwt.getClaimAsString("email"));
        newUser.setFirstName(jwt.getClaimAsString("given_name")); // Google standard field
        newUser.setLastName(jwt.getClaimAsString("family_name")); // Google standard field

        // Set Defaults
        newUser.setCreatedAt(java.time.LocalDateTime.now());

        return userService.save(newUser);
    }

    public ResponseCookie createEmptyCookie() {
        return ResponseCookie.from("AuthToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
    }

    /**
     * Creates the Secure Session Cookie (The "Golden Ticket")
     */
    public ResponseCookie createSessionCookie(String token) {
        return ResponseCookie.from("AuthToken", token)
                .httpOnly(true)     // Crucial: JS cannot read this
                .secure(true)       // HTTPS only
                .path("/")          // Available for the whole app
                .maxAge(24 * 60 * 60) // 1 Day (in seconds)
                .sameSite("Strict") // CSRF Protection
                .build();
    }

    public void validateSessionToken(String token) {
        jwtService.validateInternalToken(token);
    }

    public java.util.Optional<UserDto> getCurrentUser(String token) {
        try {
            String email = jwtService.validateInternalToken(token);
            return userService.findByEmail(email)
                    .map(userService::convertToDto);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    public record LoginResult(ResponseCookie cookie, UserDto userDto) {}
}
