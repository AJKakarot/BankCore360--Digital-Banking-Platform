package com.bankcore360.dto;

/**
 * Diagnostic response DTO for database health verification.
 *
 * @param status Overall status ("UP" or "DOWN")
 * @param database Database type ("PostgreSQL")
 * @param connection Connection verification result ("SUCCESS" or "FAILED")
 * @param message Safe operational message without exposing credentials or internal host details
 */
public record DatabaseHealthResponse(
    String status,
    String database,
    String connection,
    String message
) {}
