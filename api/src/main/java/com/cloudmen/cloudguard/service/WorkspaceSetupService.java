package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceSetupRequest;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service responsible for the initial onboarding and setup of a Google Workspace tenant within CloudGuard. <p>
 *
 * This service verifies that Domain-Wide Delegation (DWD) has been correctly configured in the customer's Google
 * Workspace environment. Upon successful verification, it links the administrative account to the organization and
 * elevates the user's privileges to SUPER_ADMIN.
 */
@Service
public class WorkspaceSetupService {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceSetupService.class);

    private final UserService userService;
    private final OrganizationService organizationService;
    private final GoogleApiFactory googleApiFactory;

    public WorkspaceSetupService(UserService userService,
                                 OrganizationService organizationService,
                                 GoogleApiFactory googleApiFactory) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.googleApiFactory = googleApiFactory;
    }

    /**
     * Initializes a new Workspace tenant by verifying DWD access and promoting the requesting user to a
     * Super Administrator.
     *
     * @param loggedInEmail the email of the user initiating the setup
     * @param request       the setup payload containing the designated admin email
     * @return the updated {@link UserDto} reflecting the new SUPER_ADMIN role
     * @throws ResponseStatusException if DWD verification fails (HTTP 403)
     */
    public UserDto initializeTenant(String loggedInEmail, WorkspaceSetupRequest request) {
        User user = userService.findByEmail(loggedInEmail);

        boolean success = verifyDwdConnection(request.adminEmail());

        if (!success) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Google Domain-Wide Delegation verification failed.");
        }

        user.setRoles(new java.util.ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
        User updatedUser = userService.save(user);

        if (user.getOrganizationId() != null) {
            organizationService.findById(user.getOrganizationId())
                    .ifPresent(org -> organizationService.updateAdminEmailForOrg(
                            request.adminEmail(), org));
        }

        return userService.convertToDto(updatedUser);
    }

    /**
     * Performs a minimal, read-only API call to verify if the CloudGuard service account has been granted Domain-Wide
     * Delegation rights by the Workspace administrator.
     */
    private boolean verifyDwdConnection(String adminEmail) {
        try {
            Directory directoryService = googleApiFactory.getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
                    adminEmail
            );

            directoryService.users().list()
                    .setCustomer("my_customer")
                    .setMaxResults(1)
                    .execute();

            return true;
        } catch (Exception e) {
            log.error("Google DWD verification failed for {}: {}", adminEmail, e.getMessage());
            return false;
        }
    }
}