package com.bankcore360.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standardized structure for API error responses.
 *
 * @param timestamp ISO timestamp when the error occurred
 * @param status HTTP status code
 * @param error HTTP status error phrase
 * @param message Human-readable error description
 * @param path Request URI path that generated the error
 * @param details Optional list of field-level or contextual error details
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, List.of());
    }

    public ErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(Instant.now(), status, error, message, path, details);
    }
}
