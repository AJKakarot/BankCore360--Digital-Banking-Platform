package com.bankcore360.event.payload;

import com.bankcore360.entity.LoanType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanApplicationSubmittedPayload(
        String applicationReference,
        Long customerId,
        LoanType loanType,
        BigDecimal requestedAmount,
        LocalDateTime submittedAt
) {}

