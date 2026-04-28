package com.cloudmen.cloudguard.exception;

/**
 * Exception thrown when an authentication or access token provided by an external service provider is invalid. <p>
 *
 * This typically occurs when a token from a third-party system (such as Google or Teamleader) is malformed,
 * has expired, or fails cryptographic signature validation.
 */
public class InvalidExternalTokenException extends RuntimeException{
    public InvalidExternalTokenException(String message) {
        super(message);
    }
}
