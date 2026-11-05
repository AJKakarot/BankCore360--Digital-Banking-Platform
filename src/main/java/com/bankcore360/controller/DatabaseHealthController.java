package com.bankcore360.controller;

import com.bankcore360.dto.DatabaseHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic controller to verify live database connectivity.
 * This diagnostic endpoint can be secured or removed once database operations are verified.
 */
@RestController
@RequestMapping("/api/health")
public class DatabaseHealthController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Diagnostic endpoint running a lightweight database ping query (`SELECT 1`).
     * Returns a safe operational status without leaking connection strings, credentials, or hostnames.
     *
     * @return DatabaseHealthResponse with HTTP 200 on success, or HTTP 503 on failure
     */
    @GetMapping("/db")
    public ResponseEntity<DatabaseHealthResponse> checkDatabaseHealth() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return ResponseEntity.ok(new DatabaseHealthResponse(
                        "UP",
                        "PostgreSQL",
                        "SUCCESS",
                        "Database connection and query verification succeeded"
                ));
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new DatabaseHealthResponse(
                    "DOWN",
                    "PostgreSQL",
                    "FAILED",
                    "Database returned unexpected query result"
            ));
        } catch (Exception ex) {
            log.error("Database connectivity diagnostic failed: {}", ex.getMessage());
            // Secure response: do not expose hostnames, user credentials, or stack traces
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new DatabaseHealthResponse(
                    "DOWN",
                    "PostgreSQL",
                    "FAILED",
                    "Database connectivity check failed. Check server logs for details."
            ));
        }
    }
}
