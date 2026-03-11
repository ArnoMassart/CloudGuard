package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.notifications.NotificationFeedbackRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications/feedback")
public class NotificationFeedbackController {
    private final NotificationFeedbackService notificationFeedbackService;
    private final JwtService jwtService;

    public NotificationFeedbackController(NotificationFeedbackService notificationFeedbackService, JwtService jwtService) {
        this.notificationFeedbackService = notificationFeedbackService;
        this.jwtService = jwtService;
    }

    @GetMapping("/keys")
    public ResponseEntity<List<String>> getFeedbackKeys(
            @CookieValue(name = "AuthToken") String token
    ) {
        String userId = jwtService.validateInternalToken(token);
        List<String> keys = notificationFeedbackService.getFeedbackKeysForUser(userId).stream().toList();
        return ResponseEntity.ok(keys);
    }

    @GetMapping
    public ResponseEntity<Boolean> hasFeedback(
            @CookieValue(name="AuthToken") String token,
            @RequestParam String source,
            @RequestParam String notificationType

    ){
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(notificationFeedbackService.hasFeedback(userId,source,notificationType));

    }

    @PostMapping
    public ResponseEntity<?> submitFeedback(
            @CookieValue(name="AuthToken") String token,
            @RequestBody NotificationFeedbackRequest request
    ){
        String userId = jwtService.validateInternalToken(token);
        notificationFeedbackService.submitFeedback(userId, request.source(), request.notificationType(), request.feedbackText());
        return ResponseEntity.ok().build();
    }
}
