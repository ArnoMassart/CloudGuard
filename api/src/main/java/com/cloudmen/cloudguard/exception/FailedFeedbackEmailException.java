package com.cloudmen.cloudguard.exception;

/** Raised when outbound feedback notification email cannot be sent after persistence attempts. */
public class FailedFeedbackEmailException extends RuntimeException {

    public FailedFeedbackEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
