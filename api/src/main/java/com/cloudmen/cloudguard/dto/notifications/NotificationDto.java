package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

/**
 * Single notification card for the UI: severity, copy, routing, and whether the user already filed feedback.
 *
 * @param id                  stable identifier (projection id as string when persisted; synthetic during live aggregation)
 * @param severity            API string {@code critical}, {@code warning}, or {@code info} ({@link com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity#toDtoString})
 * @param title               headline
 * @param description         detail body (may include counts or localized sentences)
 * @param recommendedActions bullet hints for remediation
 * @param notificationType    machine key matching projection / preference mapping
 * @param source              module section key (e.g. {@code domain-dns}, {@code shared-drives})
 * @param sourceLabel         localized module label for chips
 * @param sourceRoute         in-app path for “go fix” navigation
 * @param hasReported         {@code true} when feedback exists for this user/source/type
 * @param supportsDetails     UI may link to a drill-down where applicable
 * @param createdAt           ISO-8601 timestamp string when known from projection
 */
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
        String createdAt) {}
