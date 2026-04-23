package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.dto.workspace.WorkspaceSetupRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.WorkspaceSetupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceSetupController {

    private final WorkspaceSetupService setupService;
    private final JwtService jwtService;

    public WorkspaceSetupController(WorkspaceSetupService setupService,
                                    JwtService jwtService) {
        this.setupService = setupService;
        this.jwtService = jwtService;
    }

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