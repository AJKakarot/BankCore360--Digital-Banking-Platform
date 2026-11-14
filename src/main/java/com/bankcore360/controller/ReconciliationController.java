package com.bankcore360.controller;

import com.bankcore360.dto.*;
import com.bankcore360.entity.BankStatement;
import com.bankcore360.entity.DiscrepancyStatus;
import com.bankcore360.entity.DiscrepancyType;
import com.bankcore360.service.ReconciliationEngineService;
import com.bankcore360.service.StatementParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliation")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Bank Reconciliation Engine", description = "Endpoints for external statement ingestion, automated 4-way matching, and discrepancy investigation")
public class ReconciliationController {

    private final StatementParserService statementParserService;
    private final ReconciliationEngineService reconciliationEngineService;

    public ReconciliationController(StatementParserService statementParserService,
                                    ReconciliationEngineService reconciliationEngineService) {
        this.statementParserService = statementParserService;
        this.reconciliationEngineService = reconciliationEngineService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload external bank statement (CSV or Excel)", description = "Validates file schema, verifies SHA-256 idempotency, and stores parsed statement records")
    public ResponseEntity<StatementUploadResponse> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankName", defaultValue = "Central Clearing Bank") String bankName,
            @RequestParam(value = "accountNumber", defaultValue = "360100000001") String accountNumber,
            Authentication authentication
    ) {
        String operator = authentication != null ? authentication.getName() : "ADMIN";
        StatementUploadResponse response = statementParserService.uploadAndParseStatement(file, bankName, accountNumber, operator);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batches/{statementId}/execute")
    @Operation(summary = "Execute automated 4-way reconciliation matching", description = "Matches statement items against internal double-entry ledger entries; flags discrepancies")
    public ResponseEntity<ReconciliationBatchReport> executeReconciliation(
            @PathVariable Long statementId,
            Authentication authentication
    ) {
        String operator = authentication != null ? authentication.getName() : "ADMIN";
        ReconciliationBatchReport report = reconciliationEngineService.executeReconciliation(statementId, operator);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/statements")
    @Operation(summary = "List uploaded bank statements", description = "Returns all statement files ingested with reconciliation statuses")
    public ResponseEntity<List<BankStatement>> listStatements() {
        return ResponseEntity.ok(reconciliationEngineService.listStatements());
    }

    @GetMapping("/batches/{id}/report")
    @Operation(summary = "Get reconciliation variance report", description = "Returns full match breakdown, missing items, and discrepancy details for a batch")
    public ResponseEntity<ReconciliationBatchReport> getBatchReport(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationEngineService.getBatchReport(id));
    }

    @GetMapping("/discrepancies")
    @Operation(summary = "List reconciliation discrepancies", description = "Filters discrepancies by investigation status or discrepancy type")
    public ResponseEntity<List<DiscrepancyItemDto>> getDiscrepancies(
            @RequestParam(required = false) DiscrepancyStatus status,
            @RequestParam(required = false) DiscrepancyType type
    ) {
        return ResponseEntity.ok(reconciliationEngineService.getDiscrepancies(status, type));
    }

    @PatchMapping("/discrepancies/{id}/status")
    @Operation(summary = "Investigate & resolve reconciliation discrepancy", description = "Transitions discrepancy status with mandatory audit notes; never mutates posted ledger records")
    public ResponseEntity<DiscrepancyItemDto> resolveDiscrepancy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiscrepancyRequest request,
            Authentication authentication
    ) {
        String operator = authentication != null ? authentication.getName() : "ADMIN";
        DiscrepancyItemDto resolved = reconciliationEngineService.resolveDiscrepancy(id, request.getStatus(), request.getNotes(), operator);
        return ResponseEntity.ok(resolved);
    }
}

