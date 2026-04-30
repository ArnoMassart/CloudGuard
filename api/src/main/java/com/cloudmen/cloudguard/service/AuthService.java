package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Core service responsible for user authentication, session management, and administrative auto-promotion. <p>
 *
 * This service processes external Google ID tokens, orchestrates new user registration, and manages the lifecycle of
 * secure, HttpOnly session cookies. It also features automatic "Super Admin" discovery by cross-referencing login
 * attempts with Google Workspace administrative status via Domain-Wide Delegation (DWD).
 */
@Service
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final WorkspaceCustomerIdResolver workspaceCustomerIdResolver;
    private final GoogleApiFactory googleApiFactory;

    public AuthService(
            UserService userService,
            JwtService jwtService,
            UserRepository userRepository,
            OrganizationService organizationService,
            WorkspaceCustomerIdResolver workspaceCustomerIdResolver, GoogleApiFactory googleApiFactory) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.workspaceCustomerIdResolver = workspaceCustomerIdResolver;
        this.googleApiFactory = googleApiFactory;
    }

    /**
     * Processes a login attempt using an external Google ID token. <p>
     *
     * <b>Workflow:</b> <p>
     * 1. Decodes and validates the Google JWT.
     * 2. Retrieves or registers the user in the local database.
     * 3. Attempts to link the user to a Workspace organization
     * 4. Promotes the user to SUPER_ADMIN if they possess admin rights in Workspace.
     * 5. Issues a secure internal session cookie.
     *
     * @param externalIdToken the raw OIDC token provided by Google
     * @return a {@link LoginResult} containing the secure cookie and user profile
     */
    public LoginResult processLogin(String externalIdToken) {
        Jwt jwt = jwtService.decodeExternalToken(externalIdToken);

        String email = jwt.getClaimAsString("email");

        User user = userService.findByEmailOptional(email)
                .orElseGet(() -> registerNewUser(jwt));

        if (user.getRoles().isEmpty()) {
            user.getRoles().add(UserRole.UNASSIGNED);
            userService.save(user);
        }

        Optional<WorkspaceCustomer> resolved = workspaceCustomerIdResolver.resolveWorkspaceCustomer(email);
        resolved.ifPresent(customer -> {
            organizationService.ensureUserLinkedToOrganization(
                    user, customer.id(), customer.displayName());

            checkAndPromoteAdmin(user);
        });

        String pictureUrl = jwt.getClaimAsString("picture");
        if (pictureUrl != null && !pictureUrl.isBlank() && !pictureUrl.equals(user.getPictureUrl())) {
            user.setPictureUrl(pictureUrl);
            userService.save(user);
        }

        String sessionToken = jwtService.generateToken(user);
        ResponseCookie cookie = createSessionCookie(sessionToken);

        UserDto userDto = userService.convertToDto(user);

        return new LoginResult(cookie, userDto);
    }

    /**
     * Validates an internal session token and retrieves the corresponding user profile.
     *
     * @param token the internal JWT session token
     * @return the {@link UserDto} if valid, or {@code null} if expired/invalid
     */
    public UserDto validateSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String email = jwtService.validateInternalToken(token);

            return userRepository.findByEmail(email).map(userService::convertToDto).orElse(null);
        } catch (Exception e ) {
            return null;
        }
    }

    /**
     * Retrieves the current user context based on the session token.
     *
     * @param token the internal JWT session token
     * @return an Optional {@link UserDto}
     */
    public Optional<UserDto> getCurrentUser(String token) {
        try {
            String email = jwtService.validateInternalToken(token);

            return userService.findByEmailOptional(email)
                    .map(userService::convertToDto);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Generates a secure, HttpOnly session cookie ("The Golden Ticket").
     *
     * @param token the internal session token to wrap in the cookie
     * @return a configured {@link ResponseCookie}
     */
    public ResponseCookie createSessionCookie(String token) {
        return ResponseCookie.from("AuthToken", token)
                .httpOnly(true)     // Crucial: JS cannot read this
                .secure(true)       // HTTPS only
                .path("/")          // Available for the whole app
                .maxAge(24 * 60 * 60L) // 1 Day (in seconds)
                .sameSite("Strict") // CSRF Protection
                .build();
    }

    /**
     * Generates an expired cookie to effectively log out the user.
     *
     * @return an empty, expired {@link ResponseCookie}
     */
    public ResponseCookie createEmptyCookie() {
        return ResponseCookie.from("AuthToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
    }

    private User registerNewUser(Jwt jwt) {
        User newUser = new User();

        newUser.setEmail(jwt.getClaimAsString("email"));
        newUser.setFirstName(jwt.getClaimAsString("given_name")); // Google standard field
        newUser.setLastName(jwt.getClaimAsString("family_name")); // Google standard field
        newUser.setPictureUrl(jwt.getClaimAsString("picture")); // Google profile photo URL

        // Set Defaults
        newUser.setCreatedAt(java.time.LocalDateTime.now());

        return userService.save(newUser);
    }

    private void checkAndPromoteAdmin(User user) {
        organizationService.findById(user.getOrganizationId()).ifPresent(org -> {
            // Bepaal welk emailadres we gebruiken voor de DWD-test
            String impersonationEmail = org.getAdminEmail();
            boolean isFirstTimeSetup = false;

            if (impersonationEmail == null || impersonationEmail.isBlank()) {
                impersonationEmail = user.getEmail();
                isFirstTimeSetup = true;
            }

            try {
                // Bouw de Directory service via impersonatie
                Directory directory = googleApiFactory.getDirectoryService(
                        DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
                        impersonationEmail
                );

                // Controleer of de inloggende gebruiker een admin is in Google Workspace
                com.google.api.services.admin.directory.model.User googleUser =
                        directory.users().get(user.getEmail()).execute();

                if (Boolean.TRUE.equals(googleUser.getIsAdmin())) {
                    // 1. Promoveer de gebruiker lokaal naar SUPER_ADMIN
                    user.getRoles().clear();
                    user.getRoles().add(UserRole.SUPER_ADMIN);
                    userService.save(user);

                    // 2. Als dit de eerste keer is, sla dit adres op als de org-admin
                    if (isFirstTimeSetup) {
                        organizationService.updateAdminEmailForOrg(user.getEmail(), org);
                    }
                }
            } catch (Exception e) {
                // De impersonatie faalt waarschijnlijk omdat DWD nog niet is ingesteld [cite: 4]
                // We laten de gebruiker UNASSIGNED, de frontend stuurt ze naar /setup [cite: 12, 19]
                System.err.println("DWD auto-discovery failed for: " + user.getEmail());
            }
        });
    }
}