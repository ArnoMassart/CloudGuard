package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    private final TeamleaderService teamleaderService;
    private final TeamleaderCompanyService teamleaderCompanyService;

    public TestController(TeamleaderService teamleaderService, TeamleaderCompanyService teamleaderCompanyService) {
        this.teamleaderService = teamleaderService;
        this.teamleaderCompanyService = teamleaderCompanyService;
    }

    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API connection works! V3");
    }


    @GetMapping("/teamleader/company")
    public ResponseEntity<?> testTeamleaderCompanyFields(@RequestParam String email) {
        try {
            // 1. Maak de headers aan met het huidige access token
            HttpHeaders headers = teamleaderService.createHeaders();

            // 2. Zoek het bedrijf ID op basis van het e-mailadres
            String companyId = teamleaderCompanyService.getCompanyIdByDomain(email, headers);

            if (companyId == null) {
                return ResponseEntity.status(404).body("Geen bedrijf gevonden in Teamleader voor e-mail: " + email);
            }

            // 3. Haal de volledige payload van dit bedrijf op (inclusief alle custom fields)
            Map<String, Object> companyDetails = teamleaderCompanyService.getCompanyDetails(companyId, headers);

            // 4. Stuur de ruwe data terug. Spring maakt hier automatisch een leesbare JSON van!
            return ResponseEntity.ok(companyDetails);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fout bij ophalen Teamleader data: " + e.getMessage());
        }
    }
}
