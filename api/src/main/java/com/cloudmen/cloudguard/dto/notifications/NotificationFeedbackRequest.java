package com.cloudmen.cloudguard.dto.notifications;

public record NotificationFeedbackRequest(
        String source,
        String notificationType,
        String feedbackText
) {
}