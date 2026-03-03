package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.DomainDto;
import com.cloudmen.cloudguard.service.GoogleDomainService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/google/domains")
public class GoogleDomainController {

    private final GoogleDomainService googleDomainService;
    private final JwtService jwtService;

    public GoogleDomainController(GoogleDomainService googleDomainService, JwtService jwtService) {
        this.googleDomainService = googleDomainService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public List<DomainDto> getAllDomains(
            @CookieValue(name="AuthToken", required = false) String token
    ){
        String email = jwtService.validateInternalToken(token);
        return googleDomainService.getAllDomains(email);
    }
}
