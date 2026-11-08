package com.bankcore360.dto;

import com.bankcore360.entity.LedgerAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateLedgerAccountRequest {

    private String accountNumber;

    @NotBlank(message = "Account name is required")
    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;

    @NotNull(message = "Account type is required")
    private LedgerAccountType accountType;

    private String currency = "INR";

    private Long customerId;

    private boolean isSystemAccount = false;

    private BigDecimal initialBalance = BigDecimal.ZERO;

    public CreateLedgerAccountRequest() {
    }

    public CreateLedgerAccountRequest(String accountNumber, String name, LedgerAccountType accountType,
                                      String currency, Long customerId, boolean isSystemAccount, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.accountType = accountType;
        this.currency = currency;
        this.customerId = customerId;
        this.isSystemAccount = isSystemAccount;
        this.initialBalance = initialBalance;
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

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
