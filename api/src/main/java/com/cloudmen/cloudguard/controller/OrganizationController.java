package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.dto.organization.DatabaseOrgResponse;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller responsible for managing organizations. <p>
 *
 * This controller provides endpoints to fetch lists of all registered organizations, including both comprehensive and
 * paginated, searchable variations. <p>
 *
 * All routes are mapped under the {@code /org} prefix.
 */
@RestController
@RequestMapping("/org")
public class OrganizationController {
    private final JwtService jwtService;
    private final OrganizationService organizationService;

    /**
     * Constructs a new {@link OrganizationController} with the required services.
     *
     * @param jwtService            the service used to validate the session token
     * @param organizationService   the service handling organization data operations
     */
    public OrganizationController(JwtService jwtService, OrganizationService organizationService) {
        this.jwtService = jwtService;
        this.organizationService = organizationService;
    }

    /**
     * Retrieves a comprehensive list of all organizations. <p>
     *
     * This endpoint fetches all available organizations in the system without pagination. It is primarily used for
     * retrieving a complete dataset for administrative overviews.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link List} of all {@link Organization} entities
     */
    @GetMapping("/all")
    public ResponseEntity<List<Organization>> getAll(@CookieValue(name = "AuthToken", required = false) String token) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(organizationService.getAll());
    }

    /**
     * Retrieves a paginated and conditionally filtered list of organizations. <p>
     *
     * This endpoint supports pagination via a page token and a customizable page size. It also allows searching for
     * specific organizations using an optional query parameter.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param pageToken the token indicating which page of results to fetch
     * @param size      the maximum number of organizations to return (default is 4)
     * @param query
     */
    @GetMapping("/all-paged")
    public ResponseEntity<DatabaseOrgResponse> getAllPaged(@CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
                                                           @RequestParam(defaultValue = "4") int size,
                                                           @RequestParam(required = false) String query) {
        jwtService.validateInternalToken(token);

        return ResponseEntity.ok(organizationService.getAllPaged(pageToken, size, query));
    }
}
