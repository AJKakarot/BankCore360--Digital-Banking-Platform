package com.bankcore360.dto;

import com.bankcore360.entity.LedgerAccount;
import com.bankcore360.entity.LedgerAccountStatus;
import com.bankcore360.entity.LedgerAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerAccountResponse {

    private Long id;
    private String accountNumber;
    private String name;
    private LedgerAccountType accountType;
    private String currency;
    private LedgerAccountStatus status;
    private Long customerId;
    private boolean isSystemAccount;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LedgerAccountResponse() {
    }

    public LedgerAccountResponse(LedgerAccount account, BigDecimal currentBalance, BigDecimal availableBalance) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.name = account.getName();
        this.accountType = account.getAccountType();
        this.currency = account.getCurrency();
        this.status = account.getStatus();
        this.customerId = account.getCustomer() != null ? account.getCustomer().getId() : null;
        this.isSystemAccount = account.isSystemAccount();
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        this.availableBalance = availableBalance != null ? availableBalance : BigDecimal.ZERO;
        this.createdAt = account.getCreatedAt();
        this.updatedAt = account.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(LedgerAccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LedgerAccountStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public boolean isSystemAccount() {
        return isSystemAccount;
    }

    public void setSystemAccount(boolean systemAccount) {
        isSystemAccount = systemAccount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

