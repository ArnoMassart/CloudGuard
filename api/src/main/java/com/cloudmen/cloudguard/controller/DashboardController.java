package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.service.DashboardService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for handling dashboard-related requests. <p>
 *
 * This controller provides endpoints to fetch aggregated metrics, security scores, and high-level overviews for
 * the application dashboard. <p>
 *
 * All routes in this controller are mapped under the {@code /dashboard} prefix.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link DashboardController} with the required services.
     *
     * @param dashboardService the service used to calculate and retrieve dashboard metrics and scores
     * @param jwtService the service used to validate the user's session token and extract their email address
     */
    public DashboardController(DashboardService dashboardService, JwtService jwtService) {
        this.dashboardService = dashboardService;
        this.jwtService = jwtService;
    }

    /**
     * Retrieves the primary dashboard data, including security scores. <p>
     *
     * This endpoint expects a valid {@code AuthToken} cookie. After validating the token, it fetches the detailed
     * security score breakdown for the authenticated user's organization.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link DashboardPageResponse} with the user's security scores and
     * dashboard data
     */
    @GetMapping
    public ResponseEntity<DashboardPageResponse> getDashboardData(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(dashboardService.getDashboardSecurityScores(loggedInEmail));
    }

    /**
     * Retrieves a high-level dashboard overview. <p>
     *
     * Similar to the primary dashboard endpoint, this route validates the user's session and then fetches aggregated
     * overview statistics (such as total counts or critical alerts) for a quick status update.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link DashboardOverviewResponse} with high-level metrics and
     * statistics
     */
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(dashboardService.getDashboardOverview(loggedInEmail));
    }
}
