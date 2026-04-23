package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.dto.organization.DatabaseOrgResponse;
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

    public OrganizationController(JwtService jwtService, OrganizationService organizationService) {
        this.jwtService = jwtService;
        this.organizationService = organizationService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Organization>> getAll(@CookieValue(name = "AuthToken", required = false) String token) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(organizationService.getAll());
    }

    @GetMapping("/all-paged")
    public ResponseEntity<DatabaseOrgResponse> getAllPaged(@CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
                                                           @RequestParam(defaultValue = "4") int size,
                                                           @RequestParam(required = false) String query) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(organizationService.getAllPaged(pageToken, size, query));
    }
}
