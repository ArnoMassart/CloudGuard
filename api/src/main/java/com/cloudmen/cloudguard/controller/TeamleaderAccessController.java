package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller responsible for managing Teamleader integration and
 * access verification. <p>
 *
 * This controller provides endpoints to configure Teamleader OAuth
 * credentials and to verify if an authenticated user is authorized to
 * access the system based on Teamleader data. <p>
 *
 * All routes are mapped under the {@code /teamleader} prefix.
 */
@RestController
@RequestMapping("/teamleader")
public class TeamleaderAccessController {

    private final TeamleaderAccessService teamleaderAccessService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link TeamleaderAccessController} with the
     * required services.
     *
     * @param teamleaderAccessService the service managing Teamleader API
     * communication and access logic
     * @param jwtService              the service used to validate the
     * session token
     */
    public TeamleaderAccessController(TeamleaderAccessService teamleaderAccessService, JwtService jwtService) {
        this.teamleaderAccessService = teamleaderAccessService;
        this.jwtService = jwtService;
    }

    /**
     * Configures the Teamleader OAuth credentials for the application. <p>
     *
     * This endpoint accepts an access token and a refresh token, updating
     * the system's credentials to enable background communication with the
     * Teamleader API.
     *
     * @param tokens a map containing the {@code access_token} and
     * {@code refresh_token}
     * @return a {@link ResponseEntity} with a success message, or an error
     * status if the configuration fails
     */
    @PostMapping("/setup")
    public ResponseEntity<String> setupTeamLeader(@RequestBody Map<String, String> tokens) {
        try {
            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");

            // We roepen nu de service aan in plaats van de repository
            teamleaderAccessService.updateCredentials(accessToken, refreshToken);

            return ResponseEntity.ok("Teamleader credentials succesvol geconfigureerd via de service.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Er is een onverwachte fout opgetreden.");
        }
    }

    /**
     * Verifies whether the currently authenticated user has valid access
     * rights based on Teamleader CRM data. <p>
     *
     * This endpoint decodes the provided session cookie to identify the
     * user, then checks their status within Teamleader to determine if
     * they are authorized to use CloudGuard.
     *
     * @param token the {@code AuthToken} cookie provided by the client used
     * to authenticate the request
     * @return a {@link ResponseEntity} containing a map with a single
     * {@code hasAccess} boolean flag indicating authorization status
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        boolean hasAccess = teamleaderAccessService.hasCloudGuardAccess(loggedInEmail);

        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
