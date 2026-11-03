package com.bankcore360.event.payload;

import com.bankcore360.entity.LoanDocumentType;
import java.time.LocalDateTime;

public record KycDocumentVerifiedPayload(
        Long documentId,
        String applicationReference,
        Long customerId,
        LoanDocumentType documentType,
        String verifiedBy,
        LocalDateTime occurredAt
) {}

