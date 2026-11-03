package com.bankcore360.event.payload;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmiPaidPayload(
        String applicationReference,
        Integer installmentNumber,
        BigDecimal emiAmount,
        String paymentReference,
        LocalDateTime paidAt
) {}

