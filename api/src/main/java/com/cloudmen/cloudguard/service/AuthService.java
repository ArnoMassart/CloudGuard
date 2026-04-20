package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.LoginResult;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceCustomer;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.security.WorkspaceIdentityClaims;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final WorkspaceCustomerIdResolver workspaceCustomerIdResolver;

    public AuthService(
            UserService userService,
            JwtService jwtService,
            UserRepository userRepository,
            OrganizationService organizationService,
            WorkspaceCustomerIdResolver workspaceCustomerIdResolver) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.workspaceCustomerIdResolver = workspaceCustomerIdResolver;
    }

    public LoginResult processLogin(String externalIdToken) {
        // 1. Decode the token to get all details (Name, Picture, Email)
        Jwt jwt = jwtService.decodeGoogleToken(externalIdToken);

        String email = jwt.getClaimAsString("email");

        // 2. Try to find user, OR create a new one if missing
        User user = userService.findByEmailOptional(email)
                .orElseGet(() -> registerNewUser(jwt));

        boolean isGoogleSuperAdmin = jwtService.isGoogleAdmin(jwt);

        if (isGoogleSuperAdmin) {
            Optional<WorkspaceCustomer> resolved = workspaceCustomerIdResolver.resolveWorkspaceCustomer(email);
            String workspaceCustomerId = resolved
                    .map(WorkspaceCustomer::id)
                    .or(() -> Optional.ofNullable(jwt.getClaimAsString(WorkspaceIdentityClaims.GOOGLE_WORKSPACE_CUSTOMER_ID))
                            .filter(s -> !s.isBlank())
                            .map(String::trim))
                    .orElse(null);
            String workspaceDisplayName = resolved
                    .map(WorkspaceCustomer::displayName)
                    .orElse(null);
            organizationService.ensureUserLinkedToOrganization(user, workspaceCustomerId, workspaceDisplayName);
        }

        syncSuperAdminStatus(user, isGoogleSuperAdmin);

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

    private void syncSuperAdminStatus(User user, boolean isGoogleAdmin) {
        boolean hasSuperAdminRole = user.getRoles().contains(UserRole.SUPER_ADMIN);

        if (isGoogleAdmin) {
            if (user.getOrganizationId() != null) {
                organizationService.findById(user.getOrganizationId()).ifPresent(org -> {
                    organizationService.updateAdminEmailForOrg(user.getEmail(), org);
                });
            }

            if (!hasSuperAdminRole) {
                user.getRoles().clear();
                user.getRoles().add(UserRole.SUPER_ADMIN);
                userService.save(user);
            }
        } else {
            boolean rolesUpdated = false;

            if (user.getRoles().isEmpty()) {
                user.getRoles().add(UserRole.UNASSIGNED);
                rolesUpdated = true;
            }

            if (rolesUpdated) {
                userService.save(user);
            }
        }
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
