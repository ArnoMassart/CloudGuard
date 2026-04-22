package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.DevicePageResponse;
import com.cloudmen.cloudguard.service.GoogleDeviceService;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST controller responsible for managing Google Workspace devices. <p>
 *
 * This controller provides endpoints to fetch paginated device lists, filter devices by status or type, retrieves
 * high-level overviews, and manually trigger cache refreshes. <p>
 *
 * All routes are mapped under the {@code /google/devices} prefix.
 */
@RestController
@RequestMapping("/google/devices")
public class GoogleDeviceController {
    private final GoogleDeviceService googleDeviceService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * Constructs a new {@link GoogleDeviceController} with the required services.
     *
     * @param googleDeviceService   the service handling Google device data
     * @param jwtService            the service used to validate the session token
     * @param preferenceService     the service managing user security preferences
     */
    public GoogleDeviceController(GoogleDeviceService googleDeviceService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.googleDeviceService = googleDeviceService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Retrieves a paginated and filtered list of registered devices. <p>
     *
     * This endpoint allows clients to fetch devices based on a specific page token, page size, status,
     * and device type. It also factors in the authenticated user's disabled security preferences to tailor
     * the response.
     *
     * @param token         the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @param pageToken     the token indicating which page of results to fetch
     * @param size          the maximum number of devices to return (default is 5)
     * @param status        an optional filter to match specific device statuses
     * @param deviceType    an optional filter to match specific device types
     * @return a {@link ResponseEntity} containing a {@link DevicePageResponse} with the requested devices
     * and pagination details
     */
    @GetMapping()
    public ResponseEntity<DevicePageResponse> getDevices(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType
            ) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        Set<String> disabled = preferenceService.getDisabledPreferenceKeys(loggedInEmail);
        return ResponseEntity.ok(googleDeviceService.getDevicesPaged(loggedInEmail, pageToken, size, status, deviceType, disabled));
    }

    /**
     * Retrieves a list of all unique device types currently registered within the organization
     * (e.g., Android, ChromeOS, iOS, Windows).
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a list of unique device types as strings
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getDeviceTypes(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleDeviceService.getUniqueDeviceTypes(loggedInEmail));
    }

    /**
     * Retrieves a high-level overview of the organization's devices. <p>
     *
     * This includes aggregated metrics and statistics about device statuses. The response is customized based
     * on the user's disabled preferences.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} containing a {@link DeviceOverviewResponse} with aggregated device metrics
     */
    @GetMapping("/overview")
    public ResponseEntity<DeviceOverviewResponse> getOverview(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        return ResponseEntity.ok(googleDeviceService.getDevicesPageOverview(loggedInEmail, preferenceService.getDisabledPreferenceKeys(loggedInEmail)));
    }

    /**
     * Forces a manual refresh of the device cache for the user. <p>
     *
     * This endpoint is useful when a user want to immediately synchronize the local system with the latest
     * device data from Google Workspace, bypassing the scheduled caching intervals.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to authenticate the request
     * @return a {@link ResponseEntity} confirming that the cache was successfully refreshed
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshDevicesCache(
            @CookieValue(name = "AuthToken", required = false) String token
    ) {
        String adminEmail = jwtService.validateInternalToken(token);
        googleDeviceService.forceRefreshCache(adminEmail);
        return ResponseEntity.ok("Cache is succesvol vernieuwd!");
    }
}
