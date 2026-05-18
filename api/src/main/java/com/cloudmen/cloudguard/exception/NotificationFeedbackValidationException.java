package com.cloudmen.cloudguard.exception;

/** Validation failure for notification feedback requests (missing source, type, or body). */
public class NotificationFeedbackValidationException extends RuntimeException {

    public NotificationFeedbackValidationException(String message) {
        super(message);
    }
}
