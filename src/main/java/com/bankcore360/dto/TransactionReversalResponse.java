package com.bankcore360.dto;

import com.bankcore360.entity.TransactionReversal;

import java.time.LocalDateTime;

public class TransactionReversalResponse {

    private Long id;
    private Long originalTransactionId;
    private String originalTransactionReference;
    private Long reversalTransactionId;
    private String reversalTransactionReference;
    private String reason;
    private String reversedBy;
    private LocalDateTime createdAt;
    private LedgerTransactionResponse reversalTransactionDetails;

    public TransactionReversalResponse() {
    }

    public TransactionReversalResponse(TransactionReversal reversal) {
        this.id = reversal.getId();
        if (reversal.getOriginalTransaction() != null) {
            this.originalTransactionId = reversal.getOriginalTransaction().getId();
            this.originalTransactionReference = reversal.getOriginalTransaction().getTransactionReference();
        }
        if (reversal.getReversalTransaction() != null) {
            this.reversalTransactionId = reversal.getReversalTransaction().getId();
            this.reversalTransactionReference = reversal.getReversalTransaction().getTransactionReference();
            this.reversalTransactionDetails = new LedgerTransactionResponse(reversal.getReversalTransaction());
        }
        this.reason = reversal.getReason();
        this.reversedBy = reversal.getReversedBy();
        this.createdAt = reversal.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(Long originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getOriginalTransactionReference() {
        return originalTransactionReference;
    }

    public void setOriginalTransactionReference(String originalTransactionReference) {
        this.originalTransactionReference = originalTransactionReference;
    }

    public Long getReversalTransactionId() {
        return reversalTransactionId;
    }

    public void setReversalTransactionId(Long reversalTransactionId) {
        this.reversalTransactionId = reversalTransactionId;
    }

    public String getReversalTransactionReference() {
        return reversalTransactionReference;
    }

    public void setReversalTransactionReference(String reversalTransactionReference) {
        this.reversalTransactionReference = reversalTransactionReference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(String reversedBy) {
        this.reversedBy = reversedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LedgerTransactionResponse getReversalTransactionDetails() {
        return reversalTransactionDetails;
    }

    public void setReversalTransactionDetails(LedgerTransactionResponse reversalTransactionDetails) {
        this.reversalTransactionDetails = reversalTransactionDetails;
    }
}

