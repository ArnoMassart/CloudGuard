package com.cloudmen.cloudguard.exception;

public class InvalidExternalTokenException extends RuntimeException{
    public InvalidExternalTokenException(String message) {
        super(message);
    }
}
