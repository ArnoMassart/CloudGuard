package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/org")
public class OrganisationController {
    private final JwtService jwtService;
    private final OrganizationService organizationService;

    public OrganisationController(JwtService jwtService, OrganizationService organizationService) {
        this.jwtService = jwtService;
        this.organizationService = organizationService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Organization>> getAll(@CookieValue(name = "AuthToken", required = false) String token) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(organizationService.getAll());
    }
}
