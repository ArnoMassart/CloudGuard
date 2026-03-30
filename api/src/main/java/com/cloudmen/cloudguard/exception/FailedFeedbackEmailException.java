package com.cloudmen.cloudguard.exception;

public class FailedFeedbackEmailException extends RuntimeException{
    public FailedFeedbackEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
