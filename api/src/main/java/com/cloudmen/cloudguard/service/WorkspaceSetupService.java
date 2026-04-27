package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceSetupRequest;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkspaceSetupService {

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

    public UserDto initializeTenant(String loggedInEmail, WorkspaceSetupRequest request) {
        User user = userService.findByEmail(loggedInEmail);

        boolean success = verifyDwdConnection(request.adminEmail());

        if (!success) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Google Domain-Wide Delegation verification failed.");
        }

        // Promotie naar SUPER_ADMIN en opslaan [cite: 18]
        user.setRoles(new java.util.ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
        User updatedUser = userService.save(user);

        // Koppel admin email aan de organisatie [cite: 18]
        if (user.getOrganizationId() != null) {
            organizationService.findById(user.getOrganizationId())
                    .ifPresent(org -> organizationService.updateAdminEmailForOrg(
                            request.adminEmail(), org));
        }

        return userService.convertToDto(updatedUser);
    }

    private boolean verifyDwdConnection(String adminEmail) {
        try {
            // Gebruik de factory om een Directory service te bouwen [cite: 15]
            // We gebruiken de scope 'admin.directory.user.readonly' als test [cite: 16, 17]
            Directory directoryService = googleApiFactory.getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
                    adminEmail
            );

            // Voer een minimale API-call uit om de rechten te testen [cite: 17]
            directoryService.users().list()
                    .setCustomer("my_customer")
                    .setMaxResults(1)
                    .execute();

            return true;
        } catch (Exception e) {
            // Logs voor debugging, de 403 wordt door de controller/frontend afgehandeld
            System.err.println("Google DWD verification failed: " + e.getMessage());
            return false;
        }
    }
}