package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleOrgUnitService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.policy.OrgUnitPolicyAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Google Workspace <strong>organizational units</strong> (Admin SDK OU tree), per-OU aggregated policy
 * cards, and manual OU-cache refresh. Authenticates via the {@code AuthToken} cookie.
 */
@RestController
@RequestMapping("/google/org-units")
public class GoogleOrgUnitsController {
    private final GoogleOrgUnitService orgUnitService;
    private final OrgUnitPolicyAggregator orgUnitPolicyAggregator;
    private final JwtService jwtService;

    /**
     * @param orgUnitService           builds the OU hierarchy from cached Directory data
     * @param orgUnitPolicyAggregator  loads translated policy summaries for a selected OU path
     * @param jwtService               resolves the authenticated user from the session cookie
     */
    public GoogleOrgUnitsController(GoogleOrgUnitService orgUnitService, OrgUnitPolicyAggregator orgUnitPolicyAggregator, JwtService jwtService) {
        this.orgUnitService = orgUnitService;
        this.orgUnitPolicyAggregator = orgUnitPolicyAggregator;
        this.jwtService = jwtService;
    }

    /**
     * Returns the workspace OU tree rooted at {@code "/"}, with user counts per path from the tenant cache.
     */
    @GetMapping
    public ResponseEntity<OrgUnitNodeDto> getOrgUnits(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(orgUnitService.getOrgUnitTree(loggedInEmail));
    }

    /**
     * Returns ordered policy cards (Chrome extensions, services, shared drives, 2SV, mobile management, etc.) for the
     * given Directory {@code orgUnitPath} (defaults to {@code "/"}).
     */
    @GetMapping("/policies")
    public ResponseEntity<List<OrgUnitPolicyDto>> getOrgUnitPolicies(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(defaultValue = "/") String orgUnitPath) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(orgUnitPolicyAggregator.getPolicies(loggedInEmail, orgUnitPath));
    }

    /**
     * Forces a synchronous refresh of the OU list and per-path user counts for the tenant’s admin.
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshUsersCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        orgUnitService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
