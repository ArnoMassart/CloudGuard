package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.notifications.ResolveNotificationRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.ResolvedNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/resolved")
public class ResolvedNotificationController {

    private final ResolvedNotificationService resolvedNotificationService;
    private final JwtService jwtService;

    public ResolvedNotificationController(ResolvedNotificationService resolvedNotificationService, JwtService jwtService) {
        this.resolvedNotificationService = resolvedNotificationService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<?> markAsResolved(
            @CookieValue(name = "AuthToken") String token,
            @RequestBody ResolveNotificationRequest request
    ) {
        String userId = jwtService.validateInternalToken(token);
        resolvedNotificationService.markAsResolved(userId, request);
        return ResponseEntity.ok().build();
    }
}
