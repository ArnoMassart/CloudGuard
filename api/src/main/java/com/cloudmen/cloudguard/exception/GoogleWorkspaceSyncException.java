package com.cloudmen.cloudguard.exception;

/**
 * Exception thrown when an error occurs during the synchronization of data from Google Workspace. <p>
 *
 * This acts as a generic wrapper for various failures that can happen during the sync process, such as API rate
 * limits being exceeded, network timeouts, or unexpected data formats returned by Google's APIs.
 */
public class GoogleWorkspaceSyncException extends RuntimeException {
    public GoogleWorkspaceSyncException(String message) {
        super(message);
    }

    public GoogleWorkspaceSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
