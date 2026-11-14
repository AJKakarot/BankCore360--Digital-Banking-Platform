package com.bankcore360.controller;

import com.bankcore360.dto.internal.*;
import com.bankcore360.service.InternalAiBankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal AI Banking Service APIs", description = "Secure internal service-to-service endpoints for AI Multi-Agent tools with customer isolation")
public class InternalAiBankingController {

    private final InternalAiBankingService internalAiBankingService;
    private final String expectedServiceToken;

    public InternalAiBankingController(
            InternalAiBankingService internalAiBankingService,
            @Value("${SPRING_BOOT_SERVICE_TOKEN:bankcore360-internal-ai-token}") String expectedServiceToken
    ) {
        this.internalAiBankingService = internalAiBankingService;
        this.expectedServiceToken = expectedServiceToken;
    }

    private void validateServiceToken(String serviceToken) {
        if (serviceToken == null || !serviceToken.trim().equals(expectedServiceToken.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized internal service invocation: Invalid service token");
        }
    }

    private void validateAuthenticatedUser(String authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required X-Authenticated-User header");
        }
    }

    @GetMapping("/customer/accounts")
    @Operation(summary = "Get Customer Account Summaries", description = "Internal service tool: Returns customer's accounts and current balances")
    public ResponseEntity<List<InternalCustomerAccountDto>> getCustomerAccounts(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        validateServiceToken(serviceToken);
        validateAuthenticatedUser(authenticatedUser);
        List<InternalCustomerAccountDto> accounts = internalAiBankingService.getCustomerAccounts(authenticatedUser.trim());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/customer/spending-summary")
    @Operation(summary = "Get Customer Spending Summary", description = "Internal service tool: Returns deterministic monthly income, expenses, and category totals")
    public ResponseEntity<InternalSpendingSummaryDto> getCustomerSpendingSummary(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        validateServiceToken(serviceToken);
        validateAuthenticatedUser(authenticatedUser);
        InternalSpendingSummaryDto summary = internalAiBankingService.getCustomerSpendingSummary(authenticatedUser.trim());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/customer/transactions")
    @Operation(summary = "Get Customer Transactions", description = "Internal service tool: Returns recent ledger transactions for the customer")
    public ResponseEntity<List<InternalTransactionDetailDto>> getCustomerTransactions(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        validateServiceToken(serviceToken);
        validateAuthenticatedUser(authenticatedUser);
        List<InternalTransactionDetailDto> txns = internalAiBankingService.getCustomerTransactions(authenticatedUser.trim());
        return ResponseEntity.ok(txns);
    }

    @GetMapping("/customer/transactions/{refOrId}")
    @Operation(summary = "Get Transaction Detail with Customer Ownership Check", description = "Internal service tool: Returns transaction breakdown and verifies customer ownership")
    public ResponseEntity<InternalTransactionDetailDto> getTransactionDetail(
            @PathVariable String refOrId,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        validateServiceToken(serviceToken);
        validateAuthenticatedUser(authenticatedUser);
        InternalTransactionDetailDto txn = internalAiBankingService.getTransactionDetail(authenticatedUser.trim(), refOrId);
        return ResponseEntity.ok(txn);
    }

    @GetMapping("/customer/fraud-metadata")
    @Operation(summary = "Get Customer Fraud Metadata", description = "Internal service tool: Returns risk score, risk level, and recent triggered rules without altering flags")
    public ResponseEntity<InternalCustomerFraudMetadataDto> getCustomerFraudMetadata(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        validateServiceToken(serviceToken);
        validateAuthenticatedUser(authenticatedUser);
        InternalCustomerFraudMetadataDto metadata = internalAiBankingService.getCustomerFraudMetadata(authenticatedUser.trim());
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/loans/products")
    @Operation(summary = "Get Configured Loan Products", description = "Internal service tool: Returns live retail loan product configurations and interest rates")
    public ResponseEntity<List<InternalLoanProductDto>> getLoanProducts(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken
    ) {
        validateServiceToken(serviceToken);
        List<InternalLoanProductDto> products = internalAiBankingService.getLoanProducts();
        return ResponseEntity.ok(products);
    }
}
