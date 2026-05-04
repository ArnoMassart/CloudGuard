package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.contact.ContactMessageDto;
import com.cloudmen.cloudguard.service.ContactEmailService;
import com.cloudmen.cloudguard.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contact")
public class ContactController {
    private final JwtService jwtService;
    private final ContactEmailService contactEmailService;

    public ContactController(JwtService jwtService, ContactEmailService contactEmailService) {
        this.jwtService = jwtService;
        this.contactEmailService = contactEmailService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> processContactMessage(@CookieValue(name = "AuthToken") String token,
                                                        @RequestBody ContactMessageDto request) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        contactEmailService.sendContactEmail(loggedInEmail, request.topic(), request.subject(), request.message());

        return ResponseEntity.ok().build();
    }
}
