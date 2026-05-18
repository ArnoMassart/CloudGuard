package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.notifications.NotificationFeedbackRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User feedback on notifications: submit text once per ({@code source}, {@code notificationType}), optional staff email,
 * and discover which items already have feedback.
 *
 * @see com.cloudmen.cloudguard.service.notification.NotificationFeedbackService
 */
@RestController
@RequestMapping("/notifications/feedback")
public class NotificationFeedbackController {
    private final NotificationFeedbackService notificationFeedbackService;
    private final JwtService jwtService;

    /**
     * @param notificationFeedbackService persists {@link com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback}
     * @param jwtService                  validates {@code AuthToken} to user email key
     */
    public NotificationFeedbackController(NotificationFeedbackService notificationFeedbackService, JwtService jwtService) {
        this.notificationFeedbackService = notificationFeedbackService;
        this.jwtService = jwtService;
    }

    /** Composite keys {@code source:notificationType} that already have non-blank feedback for this user. */
    @GetMapping("/keys")
    public ResponseEntity<List<String>> getFeedbackKeys(
            @CookieValue(name = "AuthToken") String token
    ) {
        String userId = jwtService.validateInternalToken(token);
        List<String> keys = notificationFeedbackService.getFeedbackKeysForUser(userId).stream().toList();
        return ResponseEntity.ok(keys);
    }

    /** {@code true} if this user submitted feedback for the given notification identity. */
    @GetMapping
    public ResponseEntity<Boolean> hasFeedback(
            @CookieValue(name="AuthToken") String token,
            @RequestParam String source,
            @RequestParam String notificationType

    ){
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(notificationFeedbackService.hasFeedback(userId,source,notificationType));

    }

    /** Stores feedback and notifies configured staff inboxes when {@code app.feedback.notification-emails} is set. */
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
