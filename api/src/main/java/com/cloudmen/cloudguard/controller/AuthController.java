package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.TokenRequestDto;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for handling authentication and session management. <p>
 *
 * This controller provides endpoints for logging in via Google, validating active sessions, retrieving the current
 * user's profile, and logging out. <p>
 *
 * All routes in the controller are mapped under the {@code /auth} prefix.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * Constructs a new {@link AuthController} with the specified authentication service.
     *
     * @param authService the service used to process logins and manage sessions
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Logs out the current user by clearing their authentication cookie. <p>
     *
     * This endpoint checks if a valid token is present. If the user is logged in, it returns an empty cookie
     * that expires immediately, effectively ending the session.
     *
     * @param token the current {@code AuthToken} cookie, defaults to {@code "no-token"} if missing
     * @return a {@link ResponseEntity} with an HTTP 200 status and an empty cookie, or an HTTP 401 status if the user was not logged in
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(name = "AuthToken", defaultValue = "no-token") String token) {
        if (token.equals("no-token")) {
            return ResponseEntity.status(401).body("Niet ingelogd");
        }
        ResponseCookie cookie = authService.createEmptyCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    /**
     * Validates the user's current session token. <p>
     *
     * This endpoint is typically used to verify if the frontend still has a valid, unexpired session and to fetch
     * the latest user details.
     *
     * @param token the {@code AuthToken} cookie provided by the client
     * @return a {@link ResponseEntity} containing the {@link UserDto} of the authenticated user
     */
    @GetMapping("/check-session")
    public ResponseEntity<UserDto> checkSession(@CookieValue(name = "AuthToken", required = false) String token) {
        UserDto user = authService.validateSession(token);
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieves the profile information of the currently authenticated user. <p>
     *
     * Similar to session validation, but specifically returns an HTTP 401 Unauthorized status if the token is
     * invalid, missing, or if the user can no longer be found.
     *
     * @param token the {@code AuthToken} cookie provided by the client
     * @return a {@link ResponseEntity} containing the {@link UserDto}, or an empty HTTP 401 response if
     * authentication fails
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@CookieValue(name = "AuthToken", required = false) String token) {
        return authService.getCurrentUser(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Processes a user login using a Google OAuth2 token. <p>
     *
     * This endpoint receives a Google token from the client, validates it through the {@link AuthService}, registers
     * the user if necessary, and generates an internal, secure HTTP-only cookie for future requests.
     *
     * @param request a {@link TokenRequestDto} containing the Google token ID
     * @return a {@link ResponseEntity} containing the logged-in user's {@link UserDto} and a {@code Set-Cookie}
     * header with the new session
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> googleLogin(@RequestBody TokenRequestDto request) {
        LoginResult result = authService.processLogin(request.getToken());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.userDto());
    }
}
