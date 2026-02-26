package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleOrgUnitService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.policy.OrgUnitPolicyAggregator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/google/org-units")
public class GoogleOrgUnitsController {
    private final GoogleOrgUnitService orgUnitService;
    private final OrgUnitPolicyAggregator orgUnitPolicyAggregator;
    private final JwtService jwtService;

    public GoogleOrgUnitsController(GoogleOrgUnitService orgUnitService, OrgUnitPolicyAggregator orgUnitPolicyAggregator, JwtService jwtService) {
        this.orgUnitService = orgUnitService;
        this.orgUnitPolicyAggregator = orgUnitPolicyAggregator;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<OrgUnitNodeDto> getOrgUnits(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(orgUnitService.getOrgUnitTree(email));
    }

    @GetMapping("/policies")
    public ResponseEntity<List<OrgUnitPolicyDto>> getOrgUnitPolicies(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(defaultValue = "/") String orgUnitPath) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(orgUnitPolicyAggregator.getPolicies(email, orgUnitPath));
    }
}
