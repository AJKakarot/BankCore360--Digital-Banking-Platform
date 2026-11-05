package com.bankcore360.controller;

import com.bankcore360.dto.BankAccountResponse;
import com.bankcore360.dto.CreateAccountRequest;
import com.bankcore360.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public BankAccountResponse createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication
    ) {
        return bankAccountService.createAccount(request, authentication);
    }

    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BankAccountResponse> getMyAccounts(Authentication authentication) {
        return bankAccountService.getMyAccounts(authentication);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    public BankAccountResponse getAccountByNumber(
            @PathVariable String accountNumber,
            Authentication authentication
    ) {
        return bankAccountService.getAccountByNumber(accountNumber, authentication);
    }

    @PatchMapping("/{accountNumber}/activate")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public BankAccountResponse activateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) {
        return bankAccountService.activateAccount(accountNumber, authentication);
    }
}

