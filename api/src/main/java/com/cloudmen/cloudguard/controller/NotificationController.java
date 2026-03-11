package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.notifications.NotificationsResponse;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationAggregationService aggregationService;
    private final JwtService jwtService;

    public NotificationController(NotificationAggregationService aggregationService, JwtService jwtService) {
        this.aggregationService = aggregationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<NotificationsResponse> getNotifications(
            @CookieValue(name = "AuthToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(aggregationService.getNotifications(userId));
    }
}
