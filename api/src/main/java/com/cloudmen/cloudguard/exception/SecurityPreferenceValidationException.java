package com.cloudmen.cloudguard.exception;

/** Invalid preference payload (missing section/key, bad DNS importance enum, etc.). Mapped to HTTP 400 by the global handler. */
public class SecurityPreferenceValidationException extends RuntimeException {

    public SecurityPreferenceValidationException(String message) {
        super(message);
    }
}
