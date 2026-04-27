package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.cache.CacheWarmupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for triggering the cache warmup process. <p>
 *
 * This controller provides an endpoint to manually or programmatically initiate the caching of data in the
 * background. <p>
 *
 * This is particularly useful for improving application performance by preloading heavy Google Workspace data into
 * memory before the user explicitly requests it. <p>
 *
 * All routes in this controller are mapped under the {@code /cache-warmup} prefix.
 */
@RestController
@RequestMapping("/cache-warmup")
public class CacheWarmupController {
    private final CacheWarmupService warmupService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link CacheWarmupController} with the required services for token validation and
     * cache execution.
     *
     * @param warmupService the service responsible for asynchronously warming up the application caches
     * @param jwtService the service used to validate the user's session token and extract their email address
     */
    public CacheWarmupController(CacheWarmupService warmupService, JwtService jwtService) {
        this.warmupService = warmupService;
        this.jwtService = jwtService;
    }

    /**
     * Triggers an asynchronous cache warmup process for the authenticated user. <p>
     *
     * This endpoint expects a valid {@code AuthToken} cookie to verify the user's identity. Once validated,
     * it extracts the user's email and delegates the heavy data-fetching to the {@link CacheWarmupService},
     * which runs in the background. <p>
     *
     * * Because the warmup process is asynchronous, this endpoint immediately returns an HTTP 202 Accepted status
     * without waiting for the caches to finish loading.
     *
     * @param token the {@code AuthToken} cookie provided by the client,
     * used to authenticate the request
     * @return a {@link ResponseEntity} with an HTTP 202 (Accepted) status, indicating that the warmup process has
     * been successfully initiated
     */
    @PostMapping
    public ResponseEntity<Void> triggerWarmup(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        warmupService.warmupAllCachesAsync(loggedInEmail);

        return ResponseEntity.accepted().build();
    }
}
