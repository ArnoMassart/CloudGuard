package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.feedback.ResolvedNotification;
import com.cloudmen.cloudguard.dto.notifications.ResolvedNotificationDto;
import com.cloudmen.cloudguard.dto.notifications.ResolveNotificationRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.ResolvedNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping
    public ResponseEntity<List<ResolvedNotificationDto>> getResolved(
            @CookieValue(name = "AuthToken") String token
    ) {
        String userId = jwtService.validateInternalToken(token);
        List<ResolvedNotification> list = resolvedNotificationService.getResolvedForUser(userId);
        List<ResolvedNotificationDto> dtos = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ResolvedNotificationDto toDto(ResolvedNotification r) {
        return new ResolvedNotificationDto(
                "resolved-" + r.getId(),
                r.getSeverity(),
                r.getTitle(),
                r.getDescription(),
                r.getRecommendedActions() != null ? r.getRecommendedActions() : List.of(),
                r.getNotificationType(),
                r.getSource(),
                r.getSourceLabel(),
                r.getSourceRoute(),
                r.getResolvedAt() != null ? r.getResolvedAt().toString() : null
        );
    }
}
