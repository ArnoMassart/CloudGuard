package com.cloudmen.cloudguard.dto.notifications;

import java.time.LocalDateTime;

public record NotificationUserStateDto(
        Long id,
        String userId,
        Boolean read,
        Boolean resolved,
        LocalDateTime acknowledgedAt,
        String feedbackText,
        LocalDateTime feedbackCreatedAt
) {
}
