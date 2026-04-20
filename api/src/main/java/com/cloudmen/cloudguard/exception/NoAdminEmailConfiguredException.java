package com.cloudmen.cloudguard.exception;

public class NoAdminEmailConfiguredException extends RuntimeException {
    public NoAdminEmailConfiguredException(String message) {
        super(message);
    }
}
