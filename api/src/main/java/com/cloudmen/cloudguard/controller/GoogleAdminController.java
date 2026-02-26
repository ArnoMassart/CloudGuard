package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleOrgUnitService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.policy.OrgUnitPolicyAggregator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/google")
public class GoogleAdminController {
    private final GoogleOrgUnitService googleOrgUnitService;
    private final OrgUnitPolicyAggregator orgUnitPolicyAggregator;
    private final JwtService jwtService;

    public GoogleAdminController(
            GoogleOrgUnitService googleOrgUnitService,
            OrgUnitPolicyAggregator orgUnitPolicyAggregator,
            JwtService jwtService) {
        this.googleOrgUnitService = googleOrgUnitService;
        this.orgUnitPolicyAggregator = orgUnitPolicyAggregator;
        this.jwtService = jwtService;
    }

    @GetMapping("/org-units")
    public ResponseEntity<OrgUnitNodeDto> getOrgUnits(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleOrgUnitService.getOrgUnitTree(email));
    }

    @GetMapping("/org-units/policies")
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
