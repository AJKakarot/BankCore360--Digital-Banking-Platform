package com.bankcore360.controller;

import com.bankcore360.dto.*;
import com.bankcore360.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@Tag(name = "Double-Entry Ledger", description = "Enterprise Immutable Double-Entry Accounting Ledger Engine")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create Ledger Account", description = "Provisions a new chart-of-accounts ledger account")
    public ResponseEntity<LedgerAccountResponse> createAccount(@Valid @RequestBody CreateLedgerAccountRequest request) {
        LedgerAccountResponse response = ledgerService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Ledger Account by ID", description = "Fetch ledger account details and balance")
    public ResponseEntity<LedgerAccountResponse> getAccountById(@PathVariable Long id) {
        LedgerAccountResponse response = ledgerService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "List Ledger Accounts", description = "Paginated list of all chart-of-accounts ledger accounts")
    public ResponseEntity<Page<LedgerAccountResponse>> getAllAccounts(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<LedgerAccountResponse> response = ledgerService.getAllAccounts(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Record Deposit", description = "Executes balanced double-entry deposit (Debit Central Vault Asset, Credit Account Liability)")
    public ResponseEntity<LedgerTransactionResponse> deposit(
            @Valid @RequestBody LedgerDepositRequest request,
            Authentication authentication) {
        String actor = (authentication != null) ? authentication.getName() : "API_USER";
        LedgerTransactionResponse response = ledgerService.deposit(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Record Withdrawal", description = "Executes balanced double-entry withdrawal (Debit Account Liability, Credit Central Vault Asset)")
    public ResponseEntity<LedgerTransactionResponse> withdraw(
            @Valid @RequestBody LedgerWithdrawRequest request,
            Authentication authentication) {
        String actor = (authentication != null) ? authentication.getName() : "API_USER";
        LedgerTransactionResponse response = ledgerService.withdraw(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Record Fund Transfer", description = "Executes atomic double-entry transfer with deadlock-free pessimistic locking (Debit Source, Credit Destination)")
    public ResponseEntity<LedgerTransactionResponse> transfer(
            @Valid @RequestBody LedgerTransferRequest request,
            Authentication authentication) {
        String actor = (authentication != null) ? authentication.getName() : "API_USER";
        LedgerTransactionResponse response = ledgerService.transfer(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/accounts/{id}/entries")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Account Ledger Entries", description = "Paginated immutable double-entry ledger statement for an account")
    public ResponseEntity<Page<LedgerEntryResponse>> getAccountEntries(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LedgerEntryResponse> entries = ledgerService.getAccountEntries(id, pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Ledger Transaction by ID", description = "Retrieves high-level transaction and all immutable balanced entries")
    public ResponseEntity<LedgerTransactionResponse> getTransactionById(@PathVariable Long id) {
        LedgerTransactionResponse response = ledgerService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{id}/reverse")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Reverse Ledger Transaction", description = "Creates compensating inverse entries without mutating historical records")
    public ResponseEntity<TransactionReversalResponse> reverseTransaction(
            @PathVariable Long id,
            @Valid @RequestBody LedgerReversalRequest request,
            Authentication authentication) {
        String actor = (authentication != null) ? authentication.getName() : "API_USER";
        TransactionReversalResponse response = ledgerService.reverseTransaction(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
