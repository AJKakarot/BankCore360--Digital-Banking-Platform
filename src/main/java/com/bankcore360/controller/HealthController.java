package com.bankcore360.controller;

import com.bankcore360.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing system health and operational status endpoints.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private static final String APP_NAME = "BankCore360";
    private static final String STATUS_UP = "UP";

    /**
     * Endpoint to check health and availability of BankCore 360.
     *
     * @return JSON response containing service status and application name
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse(STATUS_UP, APP_NAME));
    }
}
