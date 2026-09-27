package com.example.ledger;

import com.example.ledger.transaction.IdempotencyConflictException;
import com.example.ledger.transaction.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Global exception handler — converts exceptions to structured JSON responses.
 *
 * <p>Without this, Spring returns an HTML error page or a
 * generic /error redirect. This gives the API caller a consistent
 * error body they can parse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // Error body
    // -----------------------------------------------------------------------

    public record ErrorResponse(
            int status,
            String error,
            String message,
            OffsetDateTime timestamp
    ) {
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    /**
     * Handles business-rule violations: invalid amount, account not found,
     * currency mismatch, self-transfer, etc.
     * → 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        OffsetDateTime.now()
                ));
    }

    /**
     * Handles insufficient funds on account debits / transfers / withdrawals.
     * → 422 Unprocessable Entity
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Insufficient Funds",
                        ex.getMessage(),
                        OffsetDateTime.now()
                ));
    }

    /**
     * Handles idempotency key conflicts when the same key is used with a different payload.
     * → 409 Conflict
     */
     @ExceptionHandler(IdempotencyConflictException.class)
     public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
             IdempotencyConflictException ex) {

         return ResponseEntity
                 .status(HttpStatus.CONFLICT)
                 .body(new ErrorResponse(
                         HttpStatus.CONFLICT.value(),
                         "Idempotency Conflict",
                         ex.getMessage(),
                         OffsetDateTime.now()
                 ));
     }

    /**
     * Handles domain state violations: account not active,
     * ledger balance assertion failure, etc.
     * → 422 Unprocessable Entity
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Unprocessable Entity",
                        ex.getMessage(),
                        OffsetDateTime.now()
                ));
    }

    /**
     * Handles 404 Not Found (e.g. missing route / static resource).
     * → 404 Not Found
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        "The requested endpoint does not exist or requires a different HTTP method (e.g., POST instead of GET): " + ex.getResourcePath(),
                        OffsetDateTime.now()
                ));
    }

    /**
     * Handles HTTP Method Not Supported (e.g. GET instead of POST).
     * → 405 Method Not Allowed
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "Method Not Allowed",
                        "HTTP method " + ex.getMethod() + " is not supported for this endpoint. Supported methods: " + java.util.Arrays.toString(ex.getSupportedMethods()),
                        OffsetDateTime.now()
                ));
    }

    /**
     * Safety net for anything else.
     * → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        ex.getMessage(),
                        OffsetDateTime.now()
                ));
    }
}
