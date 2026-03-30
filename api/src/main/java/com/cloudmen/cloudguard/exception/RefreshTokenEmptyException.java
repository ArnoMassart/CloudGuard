package com.cloudmen.cloudguard.exception;

public class RefreshTokenEmptyException extends RuntimeException{
    public RefreshTokenEmptyException(String message) {
        super(message);
    }
}
