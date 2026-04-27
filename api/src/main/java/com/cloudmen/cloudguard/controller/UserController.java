package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserUpdateRoleRequest;
import com.cloudmen.cloudguard.dto.users.UsersUpdateOrganizationRequest;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtService jwtService;
    private final UserService userService;
    private final CloudguardStaffService cloudguardStaffService;

    public UserController(JwtService jwtService, UserService userService, CloudguardStaffService cloudguardStaffService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.cloudguardStaffService = cloudguardStaffService;
    }

    @GetMapping("/language")
    public ResponseEntity<String> getLanguage(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getLanguage(loggedInEmail));
    }

    @PostMapping("/language")
    public ResponseEntity<Void> updateLanguage(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody Map<String, String> request) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        String newLanguage = request.get("language");

        userService.updateLanguage(loggedInEmail, newLanguage);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/request-access")
    public ResponseEntity<Boolean> getRequestRoleAccessSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getRoleRequested(loggedInEmail));
    }

    @PostMapping("/request-access")
    public ResponseEntity<String> requestRoleAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestAccess(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/no-organization")
    public ResponseEntity<Boolean> getRequestNoOrganizationSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getNoOrganizationRequested(loggedInEmail));
    }

    @PostMapping("/no-organization")
    public ResponseEntity<String> requestNoOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestNoOrganization(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/valid-role")
    public ResponseEntity<Boolean> hasValidRole(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasValidRole(loggedInEmail));
    }

    /**
     * Checks if a specific role is assigned to the authenticated user. <p>
     * * This endpoint is crucial for the frontend callback to determine if
     * the user has the 'UNASSIGNED' role and needs to be redirected to setup.
     */
    @GetMapping("/has-role")
    public ResponseEntity<Boolean> hasRole(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam UserRole role) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasRole(loggedInEmail, role));
    }

    @GetMapping("/has-organization")
    public ResponseEntity<Boolean> hasOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasOrganization(loggedInEmail));
    }

    @GetMapping("/all")
    public ResponseEntity<DatabaseUsersResponse> getAllUsers(
            @CookieValue(name="AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String orgFilter) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAll(pageToken, size, query, orgFilter));
    }

    @GetMapping("/all/no-roles")
    public ResponseEntity<DatabaseUsersResponse> getAllUsersWithRequestedRole(
            @CookieValue(name="AuthToken", required=false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String query) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAllWithRequestedRoleAndOrganization(pageToken, size, query));
    }

    @GetMapping("/all/requested-count")
    public ResponseEntity<Long> getAllRequestedCount(
            @CookieValue(name="AuthToken", required = false) String token
    ) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAllRequestedCount());
    }

    @PostMapping("/roles")
    public ResponseEntity<Void> updateRoles(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestBody UserUpdateRoleRequest request) {
        String email =jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateRoles(request.userEmail(), request.roles());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/roles-without")
    public ResponseEntity<Void> updateRolesForUserWithoutAny(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestBody UserUpdateRoleRequest request) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateRolesAndUpdateRequestedStatus(request.userEmail(), request.roles());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/org-change")
    public ResponseEntity<Void> updateUsersOrganization(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UsersUpdateOrganizationRequest request) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateUsersOrg(request.userEmail(), request.orgId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/org-setup-status")
    public ResponseEntity<Boolean> isOrganizationSetup(
            @CookieValue(name = "AuthToken", required = false) String token) {

        String loggedInEmail = jwtService.validateInternalToken(token);

        // De check of de adminEmail is ingesteld voor de organisatie van deze gebruiker
        boolean isSetup = userService.isOrganizationSetup(loggedInEmail);

        return ResponseEntity.ok(isSetup);
    }

    @GetMapping("/is-cloudmen-staff")
    public ResponseEntity<Boolean> isCloudmenStaff(
            @CookieValue(name="AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(cloudguardStaffService.isCloudmenAdmin(email));
    }
}
