package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

public record NotificationDto(
        String id,
        String severity,
        String title,
        String description,
        List<String> recommendedActions,
        String notificationType,
        String source,
        String sourceLabel,
        String sourceRoute,
        boolean hasReported,
        boolean supportsDetails,
        /** ISO-8601 instant string from {@code tbl_notifications.created_at}, or null when not persisted yet */
        String createdAt) {}
