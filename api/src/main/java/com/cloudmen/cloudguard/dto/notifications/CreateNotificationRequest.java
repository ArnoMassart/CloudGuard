package com.cloudmen.cloudguard.dto.notifications;

import com.cloudmen.cloudguard.domain.notifications.NotificationSeverity;

public record CreateNotificationRequest(
        String domainId,
        String title,
        String message,
        String recommendedAction,
        NotificationSeverity severity,
        String notificationType,
        String ticketId
) {
}
