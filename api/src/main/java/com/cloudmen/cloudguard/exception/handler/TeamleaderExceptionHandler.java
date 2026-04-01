package com.cloudmen.cloudguard.exception.handler;

import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.exception.TeamleaderResponseBodyEmptyException;
import com.cloudmen.cloudguard.exception.TokensNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TeamleaderExceptionHandler {
    @ExceptionHandler(AccessTokenEmptyException.class)
    public ResponseEntity<String> handleAccessTokenEmptyException(AccessTokenEmptyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenEmptyException.class)
    public ResponseEntity<String> handleRefreshTokenEmptyException(RefreshTokenEmptyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(TeamleaderResponseBodyEmptyException.class)
    public ResponseEntity<String> handleTeamleaderResponseBodyEmptyException(TeamleaderResponseBodyEmptyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(TokensNotFoundException.class)
    public ResponseEntity<String> handleTokensNotFoundException(TokensNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
