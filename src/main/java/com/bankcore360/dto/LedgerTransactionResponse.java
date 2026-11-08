package com.bankcore360.dto;

import com.bankcore360.entity.LedgerTransaction;
import com.bankcore360.entity.LedgerTransactionStatus;
import com.bankcore360.entity.LedgerTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LedgerTransactionResponse {

    private Long id;
    private String transactionReference;
    private String idempotencyKey;
    private LedgerTransactionType transactionType;
    private LedgerTransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String initiatedBy;
    private LocalDateTime postedAt;
    private List<LedgerEntryResponse> entries = new ArrayList<>();

    public LedgerTransactionResponse() {
    }

    public LedgerTransactionResponse(LedgerTransaction transaction) {
        this.id = transaction.getId();
        this.transactionReference = transaction.getTransactionReference();
        this.idempotencyKey = transaction.getIdempotencyKey();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.amount = transaction.getAmount();
        this.currency = transaction.getCurrency();
        this.description = transaction.getDescription();
        this.initiatedBy = transaction.getInitiatedBy();
        this.postedAt = transaction.getPostedAt();
        if (transaction.getEntries() != null) {
            this.entries = transaction.getEntries().stream()
                    .map(LedgerEntryResponse::new)
                    .toList();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LedgerTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(LedgerTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public LedgerTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerTransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public List<LedgerEntryResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryResponse> entries) {
        this.entries = entries;
    }
}
