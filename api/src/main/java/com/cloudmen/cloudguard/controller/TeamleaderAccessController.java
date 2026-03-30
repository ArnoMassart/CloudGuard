package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/teamleader")
public class TeamleaderAccessController {

    private final TeamleaderAccessService teamleaderAccessService;
    private final JwtService jwtService;

    public TeamleaderAccessController(TeamleaderAccessService teamleaderAccessService, JwtService jwtService) {
        this.teamleaderAccessService = teamleaderAccessService;
        this.jwtService = jwtService;
    }

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

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAccess(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        boolean hasAccess = teamleaderAccessService.hasCloudGuardAccess(loggedInEmail);

        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
