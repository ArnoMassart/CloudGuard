package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.security.WorkspaceIdentityClaims;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
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

    public LoginResult processLogin(String externalIdToken) {
        // 1. Decode the token to get all details (Name, Picture, Email)
        Jwt jwt = jwtService.decodeGoogleToken(externalIdToken);

        String email = jwt.getClaimAsString("email");

        // 2. Try to find user, OR create a new one if missing
        User user = userService.findByEmailOptional(email)
                .orElseGet(() -> registerNewUser(jwt));

        if (user.getRoles().isEmpty()) {
            user.getRoles().add(UserRole.UNASSIGNED); // Of UserRole.USER
            userService.save(user);
        }

        Optional<WorkspaceCustomer> resolved = workspaceCustomerIdResolver.resolveWorkspaceCustomer(email);
        resolved.ifPresent(customer -> {
            organizationService.ensureUserLinkedToOrganization(
                    user, customer.id(), customer.displayName());

            // AUTOMATISCHE SUPER-ADMIN CHECK (Multi-tenant DWD)
            checkAndPromoteAdmin(user);
        });

        // Update profile picture from JWT if available (for existing users)
        String pictureUrl = jwt.getClaimAsString("picture");
        if (pictureUrl != null && !pictureUrl.isBlank() && !pictureUrl.equals(user.getPictureUrl())) {
            user.setPictureUrl(pictureUrl);
            userService.save(user);
        }

        // 3. Generate Session Token (Same as before)
        String sessionToken = jwtService.generateToken(user);
        ResponseCookie cookie = createSessionCookie(sessionToken);

        // 4. Return result
        UserDto userDto = userService.convertToDto(user);

        return new LoginResult(cookie, userDto);
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
                .maxAge(24 * 60 * 60L) // 1 Day (in seconds)
                .sameSite("Strict") // CSRF Protection
                .build();
    }

    public java.util.Optional<UserDto> getCurrentUser(String token) {
        try {
            String email = jwtService.validateInternalToken(token);

            return userService.findByEmailOptional(email)
                    .map(userService::convertToDto);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    public String translateRoleName(String googleName) {
        return switch (googleName) {
            case "_SEED_ADMIN_ROLE" -> "Super Admin";
            case "_READ_ONLY_ADMIN_ROLE" -> "Read Only Admin";
            default -> "User";
        };
    }


}
