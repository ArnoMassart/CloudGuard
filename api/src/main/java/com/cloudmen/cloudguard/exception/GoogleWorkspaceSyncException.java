package com.cloudmen.cloudguard.exception;

public class GoogleWorkspaceSyncException extends RuntimeException {
    public GoogleWorkspaceSyncException(String message) {
        super(message);
    }

    public GoogleWorkspaceSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
