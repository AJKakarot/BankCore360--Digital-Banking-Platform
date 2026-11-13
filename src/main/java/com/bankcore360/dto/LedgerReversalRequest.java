package com.bankcore360.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LedgerReversalRequest {

    @NotBlank(message = "Reversal reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    @Size(max = 128, message = "Idempotency key must not exceed 128 characters")
    private String idempotencyKey;

    public LedgerReversalRequest() {
    }

    public LedgerReversalRequest(String reason, String idempotencyKey) {
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}

