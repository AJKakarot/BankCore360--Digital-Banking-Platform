package com.bankcore360.controller;

import com.bankcore360.dto.*;
import com.bankcore360.entity.FraudAlertStatus;
import com.bankcore360.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@Tag(name = "Fraud Detection Engine", description = "Real-time pre-settlement fraud evaluation, Redis velocity monitoring, and Kafka alerts")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Evaluate Transaction for Fraud", description = "Computes risk score (0-100), checks Redis velocity limits, and classifies risk")
    public ResponseEntity<FraudAssessmentResponse> evaluate(
            @Valid @RequestBody EvaluateFraudRequest request,
            Authentication authentication) {
        String actor = (authentication != null) ? authentication.getName() : "ANONYMOUS";
        FraudAssessmentResponse response = fraudDetectionService.evaluateTransaction(request, actor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "List Fraud Alerts", description = "Paginated list of suspicious transactions flagged for manual review")
    public ResponseEntity<Page<FraudAlertResponse>> getAlerts(
            @RequestParam(required = false) FraudAlertStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FraudAlertResponse> alerts = fraudDetectionService.getAlerts(status, pageable);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/{id}")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Fraud Alert Details", description = "Fetch alert details including triggered rules and investigation audit trail")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable Long id) {
        FraudAlertResponse response = fraudDetectionService.getAlertById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/alerts/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update Alert Status & Record Investigation", description = "Appends reviewer action and transitions alert status")
    public ResponseEntity<FraudAlertResponse> updateAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertStatusRequest request,
            Authentication authentication) {
        String reviewer = (authentication != null) ? authentication.getName() : "ADMIN";
        FraudAlertResponse response = fraudDetectionService.updateAlertStatus(id, request, reviewer);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{id}/risk-profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Account Risk Profile", description = "Calculates average risk score, active alerts, and flagging state for an account")
    public ResponseEntity<AccountRiskProfileResponse> getAccountRiskProfile(@PathVariable String id) {
        AccountRiskProfileResponse response = fraudDetectionService.getAccountRiskProfile(id);
        return ResponseEntity.ok(response);
    }
}

