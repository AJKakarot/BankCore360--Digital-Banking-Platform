package com.bankcore360.dto;

import com.bankcore360.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {

    @NotNull(message = "accountType is required")
    private AccountType accountType;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}

