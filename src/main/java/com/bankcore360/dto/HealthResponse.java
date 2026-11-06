package com.bankcore360.dto;

/**
 * Data Transfer Object representing the system health status response.
 *
 * @param status the operational status of the service (e.g., "UP")
 * @param application the name of the service ("BankCore360")
 */
public record HealthResponse(
    String status,
    String application
) {}
