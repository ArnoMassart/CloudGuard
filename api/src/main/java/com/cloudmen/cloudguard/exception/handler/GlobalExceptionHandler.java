package com.cloudmen.cloudguard.exception.handler;

import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.exception.InvalidExternalTokenException;
import com.cloudmen.cloudguard.exception.NotificationFeedbackValidationException;
import com.cloudmen.cloudguard.exception.PdfGenerationException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.exception.SecurityPreferenceValidationException;
import com.cloudmen.cloudguard.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(@Qualifier("messageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private ResponseEntity<String> plainText(HttpStatusCode status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body);
    }

    @ExceptionHandler(GoogleWorkspaceSyncException.class)
    public ResponseEntity<String> handleGoogleWorkspaceSyncException(GoogleWorkspaceSyncException ex) {
        return plainText(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<String> handlePdfGenerationException(PdfGenerationException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidExternalTokenException.class)
    public ResponseEntity<String> handleInvalidExternalTokenException(InvalidExternalTokenException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException ex) {
        return plainText(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(FailedFeedbackEmailException.class)
    public ResponseEntity<String> handleFailedFeedbackEmailException(FailedFeedbackEmailException ex) {
        return plainText(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(NotificationFeedbackValidationException.class)
    public ResponseEntity<String> handleNotificationFeedbackValidationException(
            NotificationFeedbackValidationException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SecurityPreferenceValidationException.class)
    public ResponseEntity<String> handleSecurityPreferenceValidationException(
            SecurityPreferenceValidationException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessTokenEmptyException.class)
    public ResponseEntity<String> handleAccessTokenEmptyException(AccessTokenEmptyException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenEmptyException.class)
    public ResponseEntity<String> handleRefreshTokenEmptyException(RefreshTokenEmptyException ex) {
        return plainText(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String body = ex.getReason();
        if (body == null || body.isBlank()) {
            body =
                    messageSource.getMessage(
                            "api.error.http_status",
                            new Object[] {status.value()},
                            LocaleContextHolder.getLocale());
        }
        return plainText(status, body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        return plainText(
                HttpStatus.BAD_REQUEST,
                messageSource.getMessage("api.error.invalid_request", null, LocaleContextHolder.getLocale()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnhandled(Exception ex) {
        log.error("Unhandled exception", ex);
        return plainText(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageSource.getMessage("api.error.unexpected", null, LocaleContextHolder.getLocale()));
    }
}
