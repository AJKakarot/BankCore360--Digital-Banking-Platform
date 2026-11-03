package com.bankcore360.event.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanApprovedPayload(
        String applicationReference,
        Long customerId,
        BigDecimal approvedAmount,
        BigDecimal interestRate,
        Integer approvedTenureMonths,
        String managerRemarks,
        String approvedBy,
        LocalDateTime decisionTimestamp
) {}

