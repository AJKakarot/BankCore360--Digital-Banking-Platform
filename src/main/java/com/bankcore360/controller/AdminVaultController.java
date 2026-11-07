package com.bankcore360.controller;

import com.bankcore360.dto.InjectCapitalRequest;
import com.bankcore360.dto.VaultStatusResponse;
import com.bankcore360.entity.VaultTransaction;
import com.bankcore360.service.BankVaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vault")
@Tag(name = "Bank Core Vault & Treasury", description = "Monitor Bank ₹100 Lakh Liquidity Pool, disbursements, and atomic capital injections")
public class AdminVaultController {

    private final BankVaultService bankVaultService;

    @Autowired
    public AdminVaultController(BankVaultService bankVaultService) {
        this.bankVaultService = bankVaultService;
    }

    @GetMapping({"", "/", "/status"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get Bank Vault Status", description = "View total capital (₹100 Lakh default), available liquidity, disbursed funds, and recent ledger transactions")
    public ResponseEntity<VaultStatusResponse> getVaultStatus() {
        return ResponseEntity.ok(bankVaultService.getVaultStatus());
    }

    @PostMapping("/inject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inject Capital into Vault", description = "Admin can replenish or expand bank liquidity reserve with atomic audit logging")
    public ResponseEntity<VaultTransaction> injectCapital(@Valid @RequestBody InjectCapitalRequest request, Authentication auth) {
        String adminUser = auth != null ? auth.getName() : "ADMIN";
        VaultTransaction txn = bankVaultService.injectCapital(request, adminUser);
        return ResponseEntity.ok(txn);
    }
}

