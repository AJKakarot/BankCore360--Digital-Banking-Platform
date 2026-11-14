package com.bankcore360.dto.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InternalCustomerAccountDto {
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;

    public InternalCustomerAccountDto() {}

    public InternalCustomerAccountDto(String accountNumber, String accountType, BigDecimal balance, String currency, String status, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

