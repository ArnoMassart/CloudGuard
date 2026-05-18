package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.service.GoogleDomainService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for listing Google Workspace domains (primary, secondary, aliases) with verification flags and user counts.
 * Authenticates via {@code AuthToken}.
 *
 * @see GoogleDomainService
 */
@RestController
@RequestMapping("/google/domains")
public class GoogleDomainController {

    private final GoogleDomainService googleDomainService;
    private final JwtService jwtService;

    /**
     * @param googleDomainService tenant-scoped domain cache facade
     * @param jwtService          resolves cookie to logged-in user email
     */
    public GoogleDomainController(GoogleDomainService googleDomainService, JwtService jwtService) {
        this.googleDomainService = googleDomainService;
        this.jwtService = jwtService;
    }

    /** Returns cached domains for the authenticated user’s organization (impersonated admin). */
    @GetMapping
    public ResponseEntity<List<DomainDto>> getAllDomains(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(googleDomainService.getAllDomains(loggedInEmail));
    }

    /** Forces refresh of the workspace domain cache entry for this user. */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String loggedInEmail = jwtService.validateInternalToken(token);
        googleDomainService.forceRefreshCache(loggedInEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
