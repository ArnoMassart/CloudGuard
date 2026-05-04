package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.*;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final JwtService jwtService;
    private final UserService userService;
    private final CloudguardStaffService cloudguardStaffService;

    /**
     * Constructs a new {@link UserController} with the required services.
     *
     * @param jwtService    the service used to validate the session token
     * @param userService   the service handling user data operations and business logic
     */
    public UserController(JwtService jwtService, UserService userService, CloudguardStaffService cloudguardStaffService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.cloudguardStaffService = cloudguardStaffService;
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
     * Checks if the authenticated user has already submitted a request for CloudGuard access.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if an access request is currently pending
     */
    @GetMapping("/request-access")
    public ResponseEntity<Boolean> getRequestAccessSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getAccessRequested(loggedInEmail));
    }

    /**
     * Submits a request for CloudGuard access on behalf of the authenticated user.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/request-access")
    public ResponseEntity<String> requestAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestAccess(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    /**
     * Checks if the authenticated user has already submitted a request for role assignment.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if an access request is currently pending
     */
    @GetMapping("/request-role")
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
    @PostMapping("/request-role")
    public ResponseEntity<String> requestRoleAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRoleRequestAccess(loggedInEmail);

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

    /**
     * Verifies if the authenticated user is linked to an organization.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if the user has an organization assigned
     */
    @GetMapping("/has-organization")
    public ResponseEntity<Boolean> hasOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasOrganization(loggedInEmail));
    }

    /**
     * Verifies if the authenticated user is accepted to CloudGuard.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if the user is accepted
     */
    @GetMapping("/is-accepted")
    public ResponseEntity<Boolean> isAccepted(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.isAccepted(loggedInEmail));
    }

    /**
     * Verifies if the authenticated user has been denied from CloudGuard.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating if the user has been denied
     */
    @GetMapping("/is-denied")
    public ResponseEntity<Boolean> isDenied(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.isDenied(loggedInEmail));
    }

    /**
     * Retrieves a paginated and conditionally filtered list of all users. <p>
     *
     * This endpoint is used to oversee all users across the system, with optional filters for search terms and
     * organizations.
     *
     * @param pageToken an optional token indicating which page to fetch
     * @param size      the maximum number of users to return (default is 4)
     * @param query     an optional search term to filter the user list
     * @param orgFilter an optional organization ID to filter users
     * @return a {@link ResponseEntity} containing a {@link DatabaseUsersResponse} with user and pagination data
     */
    @GetMapping("/all")
    public ResponseEntity<DatabaseUsersResponse<UserDto>> getAllUsers(
            @CookieValue(name="AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String orgFilter) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAll(pageToken, size, query, orgFilter));
    }

    /**
     * Retrieves a paginated list of users who have requested a role assignment but currently lack one.
     *
     * @param pageToken an optional token indicating which page to fetch
     * @param size      the maximum number of users to return (default is 4)
     * @param query     an optional search term to filter the list
     * @return a {@link ResponseEntity} containing a {@link DatabaseUsersResponse} with the requested users
     */
    @GetMapping("/all/requested-access")
    public ResponseEntity<DatabaseUsersResponse<UserDto>> getAllRequestedUsers(
            @CookieValue(name="AuthToken", required=false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String query) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAllRequested(pageToken, size, query));
    }

    /**
     * Retrieves the total count of users who have requested a role assignment but currently lack one.
     *
     * @return a {@link ResponseEntity} containing the count as a {@link Long}
     */
    @GetMapping("/all/requested-count")
    public ResponseEntity<Long> getAllRequestedCount(
            @CookieValue(name="AuthToken", required = false) String token
    ) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAllRequestedCount());
    }

    /**
     * Updates the assigned roles for a specific user.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param request   the payload containing the user's email and new roles
     * @return an empty {@link ResponseEntity} indicating success
     */
    @PostMapping("/roles")
    public ResponseEntity<Void> updateRoles(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestBody UserUpdateRoleRequest request) {
        String email =jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateRoles(request.userEmail(), request.roles());
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the assigned roles for a specific user who previously had no roles, and clears their pending request
     * status.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param request   the payload containing the user's email and new roles
     * @return an empty {@link ResponseEntity} indicating success
     */
    @PostMapping("/roles-without")
    public ResponseEntity<Void> updateRolesForUserWithoutAny(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestBody UserUpdateRoleRequest request) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateRolesAndUpdateRequestedStatus(request.userEmail(), request.roles());
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the organization assignment for a specific user.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param request the payload containing the user's email and the new organization ID
     * @return an empty {@link ResponseEntity} indicating success
     */
    @PostMapping("/org-change")
    public ResponseEntity<Void> updateUsersOrganization(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UsersUpdateOrganizationRequest request) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        userService.updateUsersOrg(request.userEmail(), request.orgId());

        return ResponseEntity.ok().build();
    }

    /**
     * Checks if the organization associated with the authenticated user is fully configured (e.g., Domain-Wide
     * Delegation is active).
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a boolean indicating the organization's setup status
     */
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

    @GetMapping("/access-requests/count")
    public ResponseEntity<Long> getRequestedAccessCount(@CookieValue(name="AuthToken", required = false) String token) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getRequestedAccessCount());
    }

    @GetMapping("/denied/count")
    public ResponseEntity<Long> getDeniedCount(@CookieValue(name="AuthToken", required = false) String token) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getDeniedCount());
    }

    /**
     * Set the accepted state of the user to true
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/accepted")
    public ResponseEntity<String> userAccepted(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserDecisionRequestDto request) {
        jwtService.validateInternalToken(token);

        log.info("User email: {}", request);

        userService.acceptUser(request);

        return ResponseEntity.ok().build();
    }

    /**
     * Set the denied state of the user to true
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/denied")
    public ResponseEntity<String> userDenied(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserDenyRequest request) {
        jwtService.validateInternalToken(token);

        userService.denyUser(request);

        return ResponseEntity.ok().build();
    }

    /**
     * Set the denied state of the user to false so they can again submit an access request
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return an empty {@link ResponseEntity} indicating the request was successfully logged
     */
    @PostMapping("/reaccept")
    public ResponseEntity<String> userReaccepted(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody String email) {
        jwtService.validateInternalToken(token);

        userService.reacceptUser(email);

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a paginated list of users who have been denied access from CloudGuard.
     *
     * @param pageToken an optional token indicating which page to fetch
     * @param size      the maximum number of users to return (default is 4)
     * @param query     an optional search term to filter the list
     * @return a {@link ResponseEntity} containing a {@link DatabaseUsersResponse} with the denied users
     */
    @GetMapping("/all/denied")
    public ResponseEntity<DatabaseUsersResponse<DeniedUser>> getAllDeniedUsers(
            @CookieValue(name="AuthToken", required=false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) String query) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(userService.getAllDenied(pageToken, size, query));
    }
}
