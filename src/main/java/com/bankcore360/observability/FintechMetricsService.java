package com.bankcore360.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Custom FinTech business & infrastructure metrics service publishing to Prometheus.
 */
@Service
public class FintechMetricsService {

    private final MeterRegistry meterRegistry;

    public FintechMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLedgerTransaction(String type, String status, BigDecimal amount) {
        Counter.builder("bankcore360.ledger.transactions.total")
                .tag("type", type != null ? type : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .description("Total double-entry ledger transactions processed")
                .register(meterRegistry)
                .increment();

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            meterRegistry.summary("bankcore360.ledger.transfer.amount")
                    .record(amount.doubleValue());
        }
    }

    public void recordFraudEvaluation(String decision, String riskLevel) {
        Counter.builder("bankcore360.fraud.evaluations.total")
                .tag("decision", decision != null ? decision : "UNKNOWN")
                .tag("risk_level", riskLevel != null ? riskLevel : "UNKNOWN")
                .description("Total real-time fraud assessments executed")
                .register(meterRegistry)
                .increment();
    }

    public void recordReconciliationDiscrepancy(String discrepancyType) {
        Counter.builder("bankcore360.reconciliation.discrepancies.total")
                .tag("discrepancy_type", discrepancyType != null ? discrepancyType : "UNKNOWN")
                .description("Total reconciliation discrepancies identified during statement matching")
                .register(meterRegistry)
                .increment();
    }

    public void recordReconciliationBatch(String status, int matched, int unmatched) {
        Counter.builder("bankcore360.reconciliation.batches.total")
                .tag("status", status != null ? status : "UNKNOWN")
                .description("Total bank reconciliation batches executed")
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitThrottled(String endpoint, String clientKey) {
        Counter.builder("bankcore360.ratelimit.throttled.total")
                .tag("endpoint", endpoint != null ? endpoint : "UNKNOWN")
                .description("Total requests throttled by Redis distributed rate limiter")
                .register(meterRegistry)
                .increment();
    }

    public void recordMfaAttempt(String type, boolean success) {
        Counter.builder("bankcore360.auth.mfa.attempts.total")
                .tag("type", type != null ? type : "OTP")
                .tag("result", success ? "SUCCESS" : "FAILURE")
                .description("Multi-factor authentication validation attempts")
                .register(meterRegistry)
                .increment();
    }

    public void recordTokenReuseDetected(String username) {
        Counter.builder("bankcore360.auth.token.reuse.detected.total")
                .description("Critical security metric: refresh token reuse attacks intercepted")
                .register(meterRegistry)
                .increment();
    }
}
