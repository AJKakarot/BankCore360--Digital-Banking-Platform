package com.bankcore360.dto;

import com.bankcore360.entity.AccountStatus;
import com.bankcore360.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankAccountResponse {

    private Long id;
    private String accountNumber;
    private Long customerId;
    private String customerName;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;

    public BankAccountResponse() {
    }

    public BankAccountResponse(Long id, String accountNumber, Long customerId, String customerName,
                               AccountType accountType, BigDecimal balance,
                               AccountStatus accountStatus, LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.accountType = accountType;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

