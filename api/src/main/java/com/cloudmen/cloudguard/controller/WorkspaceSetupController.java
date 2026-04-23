package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceSetupRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.WorkspaceSetupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for the initial configuration of the Google Workspace integration for a tenant. <p>
 *
 * This controller provides endpoints to initialize Domain-Wide Delegation (DWD) for an organization, linking it to a
 * Google Admin account and promoting the initial setup user to a Super Admin. <p>
 *
 * All routes are mapped unde the {@code /api/workspace} prefix.
 */
@RestController
@RequestMapping("/api/workspace")
public class WorkspaceSetupController {

    private final WorkspaceSetupService setupService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link WorkspaceSetupController} with the required services.
     * @param setupService  the service handling the multi-tenant initialization logic and Google API verification
     * @param jwtService    the service used to validate the session token
     */
    public WorkspaceSetupController(WorkspaceSetupService setupService,
                                    JwtService jwtService) {
        this.setupService = setupService;
        this.jwtService = jwtService;
    }

    /**
     * Completes the organization setup by verifying Domain-Wide Delegation and promoting the admin user. <p>
     *
     * This endpoint validates the provided Google Admin email, ensures that the system can communicate with the
     * Google Directory API via impersonation, and updates the local organization and user roles accordingly.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param request   the payload containing the {@code adminEmail} to be configured for the organization
     * @return a {@link ResponseEntity} containing the updated {@link UserDto} with newly assigned administrative roles
     */
    @PostMapping("/setup")
    public ResponseEntity<UserDto> setupWorkspace(
            @CookieValue(value = "AuthToken", required = false) String token,
            @RequestBody WorkspaceSetupRequest request) {

        String loggedInEmail = jwtService.validateInternalToken(token);

        // De service handelt de verificatie en DB-updates af
        UserDto updatedUser = setupService.initializeTenant(loggedInEmail, request);

        return ResponseEntity.ok(updatedUser);
    }
}