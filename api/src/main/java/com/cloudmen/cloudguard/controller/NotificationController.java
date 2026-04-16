package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.notifications.NotificationsResponse;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.notification.NotificationProjectionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationAggregationService aggregationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final NotificationProjectionSyncService projectionSyncService;

    public NotificationController(
            NotificationAggregationService aggregationService,
            JwtService jwtService,
            UserRepository userRepository,
            NotificationProjectionSyncService projectionSyncService) {
        this.aggregationService = aggregationService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.projectionSyncService = projectionSyncService;
    }

    @GetMapping
    public ResponseEntity<NotificationsResponse> getNotifications(
            @CookieValue(name = "AuthToken", required = false) String token) {
        String userId = jwtService.validateInternalToken(token);
        return ResponseEntity.ok(aggregationService.getNotifications(userId, LocaleContextHolder.getLocale()));
    }

    /**
     * Reconciles persisted notification rows with the current workspace snapshot for the caller's organization.
     * No-op when the user has no organization. Returns 204 when sync completes or is skipped; 500 if sync throws.
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> syncNotifications(@CookieValue(name = "AuthToken", required = false) String token) {
        String userId = jwtService.validateInternalToken(token);
        try {
            userRepository
                    .findByEmail(userId)
                    .map(User::getOrganizationId)
                    .filter(organizationId -> organizationId != null)
                    .ifPresent(projectionSyncService::syncOrganization);
        } catch (Exception e) {
            log.error("Notification projection sync failed for user {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.noContent().build();
    }
}
