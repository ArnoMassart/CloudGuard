package com.cloudmen.cloudguard.dto.notifications;

/**
 * POST body for {@link com.cloudmen.cloudguard.controller.NotificationFeedbackController#submitFeedback}.
 *
 * @param source             notification {@code source} (same as {@link com.cloudmen.cloudguard.dto.notifications.NotificationDto#source()})
 * @param notificationType   notification type key
 * @param feedbackText       user comment (required on first substantive submit)
 */
public record NotificationFeedbackRequest(
        String source,
        String notificationType,
        String feedbackText
) {
}
