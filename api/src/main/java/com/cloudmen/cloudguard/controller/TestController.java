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

/**
 * REST controller used for testing and debugging purposes. <p>
 *
 * This controller provides simple health checks endpoints and utilities to verify integrations with third-party
 * services, such as fetching raw company data from Teamleader to inspect custom fields. <p>
 *
 * All routes are mapped under the {@code /test} prefix.
 */
@RestController
@RequestMapping("/test")
public class TestController {
    private final TeamleaderService teamleaderService;
    private final TeamleaderCompanyService teamleaderCompanyService;

    /**
     * Constructs a new {@link TestController} with the required services.
     *
     * @param teamleaderService         the core service for Teamleader API authentication and headers
     * @param teamleaderCompanyService  the service handling Teamleader company data retrieval
     */
    public TestController(TeamleaderService teamleaderService, TeamleaderCompanyService teamleaderCompanyService) {
        this.teamleaderService = teamleaderService;
        this.teamleaderCompanyService = teamleaderCompanyService;
    }

    /**
     * A simple health check endpoint to verify that the API is reachable. <p>
     *
     * This endpoint requires no authentication or parameters and simply returns a static confirmation message.
     *
     * @return a {@link ResponseEntity} containing a success string confirming the connection
     */
    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API connection works! V3");
    }


    /**
     * Retrieves the raw, unmapped company details from Teamleader based on a provided email address. <p>
     *
     * This endpoint is primarily used for debugging and inspecting the structure of the Teamleader API response,
     * especially to identify the internal IDs of custom fields configured within the Teamleader CRM.
     *
     * @param email the email address used to extract the domain and find the associated company in Teamleader
     * @return a {@link ResponseEntity} containing the raw JSON map of company details, or an error message if
     * the company is not found or the call fails
     */
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
