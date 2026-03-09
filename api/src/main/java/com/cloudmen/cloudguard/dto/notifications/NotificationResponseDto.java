package com.cloudmen.cloudguard.dto.notifications;

import com.cloudmen.cloudguard.domain.notifications.NotificationSeverity;
import com.cloudmen.cloudguard.domain.notifications.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationResponseDto(
        Long id,
        String domainId,
        String title,
        String message,
        String recommendedAction,
        NotificationSeverity severity,
        NotificationStatus status,
        String notificationType,
        String ticketId,
        LocalDateTime createdAt,
        List<NotificationUserStateDto> userStates
) {
}
