package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.notifications.DismissNotificationRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.DismissedNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/dismissed")
public class DismissedNotificationController {

    private final DismissedNotificationService dismissedNotificationService;
    private final JwtService jwtService;

    public DismissedNotificationController(DismissedNotificationService dismissedNotificationService, JwtService jwtService) {
        this.dismissedNotificationService = dismissedNotificationService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<?> markAsDismissed(
            @CookieValue(name = "AuthToken") String token,
            @RequestBody DismissNotificationRequest request
    ) {
        String userId = jwtService.validateInternalToken(token);
        dismissedNotificationService.markAsDismissed(userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> unDismiss(
            @CookieValue(name = "AuthToken") String token,
            @RequestParam String source,
            @RequestParam String notificationType
    ) {
        String userId = jwtService.validateInternalToken(token);
        boolean removed = dismissedNotificationService.unDismiss(userId, source, notificationType);
        return removed ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
