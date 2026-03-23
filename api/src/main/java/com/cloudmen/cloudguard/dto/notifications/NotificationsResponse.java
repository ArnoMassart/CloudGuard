package com.cloudmen.cloudguard.dto.notifications;

import java.util.List;

public record NotificationsResponse(
        List<NotificationDto> active,
        List<NotificationDto> dismissed
) {
}
