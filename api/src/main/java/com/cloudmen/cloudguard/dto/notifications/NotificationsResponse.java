package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

/**
 * @param lastNotificationSyncAt ISO-8601 instant string (e.g. from {@link java.time.Instant#toString()}), or null if never synced
 */
public record NotificationsResponse(
        List<NotificationDto> active, List<NotificationDto> solved, String lastNotificationSyncAt) {}
