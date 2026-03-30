package com.cloudmen.cloudguard.exception;

public class AccessTokenEmptyException extends RuntimeException{
    public AccessTokenEmptyException(String message) {
        super(message);
    }
}
