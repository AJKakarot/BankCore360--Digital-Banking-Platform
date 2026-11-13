package com.bankcore360.dto;

import com.bankcore360.entity.LedgerEntry;
import com.bankcore360.entity.LedgerEntryDirection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntryResponse {

    private Long id;
    private String entryReference;
    private Long transactionId;
    private String transactionReference;
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private LedgerEntryDirection direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private int entryOrder;
    private String narration;
    private LocalDateTime createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(LedgerEntry entry) {
        this.id = entry.getId();
        this.entryReference = entry.getEntryReference();
        if (entry.getTransaction() != null) {
            this.transactionId = entry.getTransaction().getId();
            this.transactionReference = entry.getTransaction().getTransactionReference();
        }
        if (entry.getAccount() != null) {
            this.accountId = entry.getAccount().getId();
            this.accountNumber = entry.getAccount().getAccountNumber();
            this.accountName = entry.getAccount().getName();
        }
        this.direction = entry.getDirection();
        this.amount = entry.getAmount();
        this.balanceAfter = entry.getBalanceAfter();
        this.entryOrder = entry.getEntryOrder();
        this.narration = entry.getNarration();
        this.createdAt = entry.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntryReference() {
        return entryReference;
    }

    public void setEntryReference(String entryReference) {
        this.entryReference = entryReference;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public LedgerEntryDirection getDirection() {
        return direction;
    }

    public void setDirection(LedgerEntryDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public int getEntryOrder() {
        return entryOrder;
    }

    public void setEntryOrder(int entryOrder) {
        this.entryOrder = entryOrder;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

