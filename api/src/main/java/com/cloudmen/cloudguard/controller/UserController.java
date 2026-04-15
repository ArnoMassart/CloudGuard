package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserUpdateRoleRequest;
import com.cloudmen.cloudguard.dto.users.UsersUpdateOrganizationRequest;
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

    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
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

    @GetMapping("/has-organization")
    public ResponseEntity<Boolean> hasOrganization(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(userService.hasOrganization(loggedInEmail));
    }

    @GetMapping("/all")
    public ResponseEntity<DatabaseUsersResponse> getAllUsers(@RequestParam(required = false) String pageToken,
                                                             @RequestParam(defaultValue = "4") int size,
                                                             @RequestParam(required = false) String query) {
        return ResponseEntity.ok(userService.getAll(pageToken, size, query));
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

    @PostMapping("/roles")
    public ResponseEntity<Void> updateRoles(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserUpdateRoleRequest request) {
       jwtService.validateInternalToken(token);

        userService.updateRoles(request.userEmail(), request.roles());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/roles-without")
    public ResponseEntity<Void> updateRolesForUserWithoutAny(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UserUpdateRoleRequest request) {
        jwtService.validateInternalToken(token);

        userService.updateRolesAndUpdateRequestedStatus(request.userEmail(), request.roles());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/org-change")
    public ResponseEntity<Void> updateUsersOrganization(@CookieValue(name = "AuthToken", required = false) String token, @RequestBody UsersUpdateOrganizationRequest request) {
        jwtService.validateInternalToken(token);

        userService.updateUsersOrg(request.userEmail(), request.orgId());

        return ResponseEntity.ok().build();
    }
}
