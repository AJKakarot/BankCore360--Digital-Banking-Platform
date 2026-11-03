package com.bankcore360.event.payload;

import java.time.LocalDateTime;

public record LoanRejectedPayload(
        String applicationReference,
        Long customerId,
        String rejectionReason,
        String rejectedBy,
        LocalDateTime rejectedAt
) {}

