package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

/**
 * Pagelike bundle for the notifications view.
 *
 * @param active                 open issues according to live rules + projection, after RBAC and preference filtering
 * @param solved                 historical SOLVED projection rows (empty when projection read is disabled or never synced)
 * @param lastNotificationSyncAt ISO-8601 instant string when the org last ran projection sync, or {@code null}
 */
public record NotificationsResponse(
        List<NotificationDto> active, List<NotificationDto> solved, String lastNotificationSyncAt) {}
