package com.bankcore360.event.payload;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmiOverduePayload(
        String applicationReference,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal emiAmount,
        int daysOverdue
) {}

