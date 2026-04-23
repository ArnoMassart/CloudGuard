package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserUpdateRoleRequest;
import com.cloudmen.cloudguard.dto.users.UsersUpdateOrganizationRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller responsible for managing user profiles, roles and access requests. <p>
 *
 * This controller provides endpoints to handle user preferences (like language), process requests for application
 * access or organization assignment, and manage administrative tasks such as role updates and organization
 * transfers. <p>
 *
 * All routes are mapped under the {@code /user} prefix.
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Constructs a new {@link UserController} with the required services.
     *
     * @param jwtService    the service used to validate the session token
     * @param userService   the service handling user data operations and business logic
     */
    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Retrieves the preferred language setting of the authenticated user.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing the user's language code
     */
    @GetMapping("/language")
    public ResponseEntity<String> getLanguage(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getLanguage(loggedInEmail));
    }

    /**
     * Updates the preferred language setting for the authenticated user.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param request   a map containing the new {@code language} code
     * @return an empty {@link ResponseEntity} indicating a successful update
     */
    @PostMapping("/language")
    public ResponseEntity<Void> updateLanguage(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody Map<String, String> request) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        String newLanguage = request.get("language");

        userService.updateLanguage(loggedInEmail, newLanguage);

        return ResponseEntity.ok().build();
    }

    /**
     * Checks if the authenticated user has already submitted a request for role assignment.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if an access request is currently pending
     */
    @GetMapping("/request-access")
    public ResponseEntity<Boolean> getRequestRoleAccessSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getRoleRequested(loggedInEmail));
    }

    /**
     * Submits a request for role assignment on behalf of the authenticated user.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/request-access")
    public ResponseEntity<String> requestRoleAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestAccess(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    /**
     * Checks if the authenticated user has already submitted a request indicating they lack an organization
     * assignment.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if a "no organization" request is pending
     */
    @GetMapping("/no-organization")
    public ResponseEntity<Boolean> getRequestNoOrganizationSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getNoOrganizationRequested(loggedInEmail));
    }

    /**
     * Submits a request on behalf of the authenticated user indicating they are not linked to any organization.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/no-organization")
    public ResponseEntity<String> requestNoOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestNoOrganization(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    /**
     * Verifies whether the authenticated user has a valid, active role assigned to their account
     * (excluding 'UNASSIGNED').
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if the user has a valid role
     */
    @GetMapping("/valid-role")
    public ResponseEntity<Boolean> hasValidRole(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasValidRole(loggedInEmail));
    }

    /**
     * Verifies
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/has-organization")
    public ResponseEntity<Boolean> hasOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasOrganization(loggedInEmail));
    }

    @GetMapping("/all")
    public ResponseEntity<DatabaseUsersResponse> getAllUsers(@RequestParam(required = false) String pageToken,
                                                             @RequestParam(defaultValue = "4") int size,
                                                             @RequestParam(required = false) String query,
                                                             @RequestParam(required = false) String orgFilter) {
        return ResponseEntity.ok(userService.getAll(pageToken, size, query, orgFilter));
    }

    @GetMapping("/all/no-roles")
    public ResponseEntity<DatabaseUsersResponse> getAllUsersWithRequestedRole(@RequestParam(required = false) String pageToken,
                                                             @RequestParam(defaultValue = "4") int size,
                                                             @RequestParam(required = false) String query) {
        return ResponseEntity.ok(userService.getAllWithRequestedRoleAndOrganization(pageToken, size, query));
    }

    @GetMapping("/all/requested-count")
    public ResponseEntity<Long> getAllRequestedCount() {
        return ResponseEntity.ok(userService.getAllRequestedCount());
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/roles")
    public ResponseEntity<Void> updateRoles(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserUpdateRoleRequest request) {
       jwtService.validateInternalToken(token);

        userService.updateRoles(request.userEmail(), request.roles());

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/roles-without")
    public ResponseEntity<Void> updateRolesForUserWithoutAny(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserUpdateRoleRequest request) {
        jwtService.validateInternalToken(token);

        userService.updateRolesAndUpdateRequestedStatus(request.userEmail(), request.roles());

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/org-change")
    public ResponseEntity<Void> updateUsersOrganization(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UsersUpdateOrganizationRequest request) {
        jwtService.validateInternalToken(token);

        userService.updateUsersOrg(request.userEmail(), request.orgId());

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/org-setup-status")
    public ResponseEntity<Boolean> isOrganizationSetup(
            @CookieValue(name = "AuthToken", required = false) String token) {

        String loggedInEmail = jwtService.validateInternalToken(token);

        // De check of de adminEmail is ingesteld voor de organisatie van deze gebruiker
        boolean isSetup = userService.isOrganizationSetup(loggedInEmail);

        return ResponseEntity.ok(isSetup);
    }
}
