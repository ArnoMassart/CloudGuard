package com.cloudmen.cloudguard.exception;

/**
 * Thrown when org-scoped preference writes are attempted but the user has no {@code organization_id} yet.
 */
public class OrganizationRequiredException extends RuntimeException {

    public OrganizationRequiredException(String message) {
        super(message);
    }
}
