package com.cloudmen.cloudguard.domain.feedback;

public record NotificationFeedbackRequest(
        String source,
        String notificationType,
        String feedbackText
) {
}