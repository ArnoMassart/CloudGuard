package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

public record ResolvedNotificationDto(
        String id,
        String severity,
        String title,
        String description,
        List<String> recommendedActions,
        String notificationType,
        String source,
        String sourceLabel,
        String sourceRoute,
        String resolvedAt
) {}
