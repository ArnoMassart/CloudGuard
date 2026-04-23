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
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtService jwtService;
    private final UserService userService;

    /**
     *
     */
    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/language")
    public ResponseEntity<String> getLanguage(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getLanguage(loggedInEmail));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/language")
    public ResponseEntity<Void> updateLanguage(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody Map<String, String> request) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        String newLanguage = request.get("language");

        userService.updateLanguage(loggedInEmail, newLanguage);

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/request-access")
    public ResponseEntity<Boolean> getRequestRoleAccessSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getRoleRequested(loggedInEmail));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/request-access")
    public ResponseEntity<String> requestRoleAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestAccess(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/no-organization")
    public ResponseEntity<Boolean> getRequestNoOrganizationSent(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.getNoOrganizationRequested(loggedInEmail));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @PostMapping("/no-organization")
    public ResponseEntity<String> requestNoOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        userService.updateRequestNoOrganization(loggedInEmail);

        return ResponseEntity.ok().build();
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/valid-role")
    public ResponseEntity<Boolean> hasValidRole(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasValidRole(loggedInEmail));
    }

    /**
     *
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     */
    @GetMapping("/has-role")
    public ResponseEntity<Boolean> hasRole(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam UserRole role) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasRole(loggedInEmail, role));
    }

    /**
     *
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
