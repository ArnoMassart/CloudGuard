package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthPagedResponse;
import com.cloudmen.cloudguard.service.GoogleOAuthService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for managing Google Workspace OAuth application access and tokens. <p>
 *
 * This controller provides endpoints to fetch paginated lists of internal or third-party applications that have
 * OAuth access to the organization's data, retrieve high-level overviews, and manually refresh the OAuth cache. <p>
 *
 * All routes are mapped under the {@code /google/oAuth} prefix.
 */
@RestController
@RequestMapping("/google/oAuth")
public class GoogleOAuthController {
    private final GoogleOAuthService oAuthService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * Constructs a new {@link GoogleOAuthController} with the required services.
     *
     * @param oAuthService          the service handling Google OAuth data and caching
     * @param jwtService            the service used to validate the session token
     * @param preferenceService     the service managing user security preferences
     */
    public GoogleOAuthController(GoogleOAuthService oAuthService, JwtService jwtService,
                                UserSecurityPreferenceService preferenceService) {
        this.oAuthService = oAuthService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Retrieves a paginated and filtered list of internal or third-party OAuth tokens and connected applications. <p>
     *
     * This endpoint allows clients to search through authorized applications based on a query string or risk level.
     * Pagination is supported via a page token and a customizable page size.
     *
     * @param token     the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param pageToken the token indicating which page of results to fetch
     * @param size      the maximum number of tokens to return (default is 3)
     * @param query     an optional search term to filter applications by name
     * @param risk      an optional filter to match specific risk levels (e.g., HIGH, LOW)
     * @return a {@link ResponseEntity} containing a {@link OAuthPagedResponse} with the requested tokens and
     * pagination details
     */
    @GetMapping
    public ResponseEntity<OAuthPagedResponse> getTokens(@CookieValue(name = "AuthToken", required = false) String token,
                                                       @RequestParam(required = false) String pageToken,
                                                       @RequestParam(defaultValue = "3") int size,
                                                       @RequestParam(required = false) String query,
                                                       @RequestParam(required = false) String risk
                                                       ) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(oAuthService.getOAuthPaged(loggedInEmail, pageToken, size, query, risk));
    }

    /**
     * Retrieves a high-level overview of the organization's internal or third-party OAuth application access. <p>
     *
     * This includes aggregated metrics such as total connected apps and breakdowns by risk level. The response
     * is tailored based on the authenticated user's disabled security preferences.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link OAuthOverviewResponse} with aggregated OAuth metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<OAuthOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token
                                                          ) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(oAuthService.getOAuthPageOverview(loggedInEmail,
                preferenceService.getDisabledPreferenceKeys(loggedInEmail)));
    }

    /**
     * Forces a manual refresh of the OAuth application cache for the user. <p>
     *
     * This endpoint is useful when a user want to immediately synchronize the local system with the latest
     * OAuth data from Google Workspace, bypassing the scheduled caching intervals.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} confirming that the cache was successfully refreshed
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshOAuthCache(@CookieValue(name = "AuthToken", required = false) String token
                                                          ) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        oAuthService.forceRefreshCache(loggedInEmail);

        return ResponseEntity.ok("Cache is succesvol vernieuwd!");

    }
}
