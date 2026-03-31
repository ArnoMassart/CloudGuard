package com.cloudmen.cloudguard.exception.handler;

import com.cloudmen.cloudguard.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GoogleWorkspaceSyncException.class)
    public ResponseEntity<String> handleGoogleWorkspaceSyncException(GoogleWorkspaceSyncException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<String> handlePdfGenerationException(PdfGenerationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidExternalTokenException.class)
    public ResponseEntity<String> handleInvalidExternalTokenException(InvalidExternalTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(FailedFeedbackEmailException.class)
    public ResponseEntity<String> handleFailedFeedbackEmailException(FailedFeedbackEmailException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

    @ExceptionHandler(NotificationFeedbackValidationException.class)
    public ResponseEntity<String> handleNotificationFeedbackValidationException(NotificationFeedbackValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SecurityPreferenceValidationException.class)
    public ResponseEntity<String> handleSecurityPreferenceValidationException(SecurityPreferenceValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
