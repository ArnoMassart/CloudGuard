package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

public record DismissNotificationRequest(
        String source,
        String notificationType,
        String sourceLabel,
        String sourceRoute,
        String title,
        String description,
        String severity,
        List<String> recommendedActions
) {}
