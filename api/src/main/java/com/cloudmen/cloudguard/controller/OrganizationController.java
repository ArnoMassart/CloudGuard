package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.dto.organization.DatabaseOrgResponse;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/org")
public class OrganizationController {
    private final JwtService jwtService;
    private final OrganizationService organizationService;
    private final CloudguardStaffService cloudguardStaffService;

    public OrganizationController(JwtService jwtService, OrganizationService organizationService, CloudguardStaffService cloudguardStaffService) {
        this.jwtService = jwtService;
        this.organizationService = organizationService;
        this.cloudguardStaffService = cloudguardStaffService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Organization>> getAll(@CookieValue(name = "AuthToken", required = false) String token) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);

        return ResponseEntity.ok(organizationService.getAll());
    }

    @GetMapping("/all-paged")
    public ResponseEntity<DatabaseOrgResponse> getAllPaged(@CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
                                                           @RequestParam(defaultValue = "4") int size,
                                                           @RequestParam(required = false) String query) {
        String email = jwtService.validateInternalToken(token);
        cloudguardStaffService.requireCloudmenAdmin(email);
        return ResponseEntity.ok(organizationService.getAllPaged(pageToken, size, query));
    }
}
