package com.bankcore360.controller;

import com.bankcore360.dto.DisbursementResponse;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.ManagerDecisionRequest;
import com.bankcore360.service.ManagerLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@Tag(name = "Manager Workflow", description = "Endpoints for loan application final decision (approval/rejection) and simulated disbursement")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerLoanController {

    private final ManagerLoanService managerLoanService;

    public ManagerLoanController(ManagerLoanService managerLoanService) {
        this.managerLoanService = managerLoanService;
    }

    @GetMapping("/loans/pending")
    @Operation(summary = "Get applications pending manager approval", description = "Returns all applications in PENDING_MANAGER_APPROVAL status")
    public ResponseEntity<List<LoanApplicationResponse>> getPendingApplications() {
        List<LoanApplicationResponse> applications = managerLoanService.getPendingApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/loans/approved")
    @Operation(summary = "Get approved applications ready for disbursement", description = "Returns all applications in APPROVED status")
    public ResponseEntity<List<LoanApplicationResponse>> getApprovedApplications() {
        List<LoanApplicationResponse> applications = managerLoanService.getApprovedApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/loans/{applicationId}")
    @Operation(summary = "Get loan application details for manager review", description = "Returns full application data, customer details, and checker remarks")
    public ResponseEntity<LoanApplicationResponse> getApplicationById(@PathVariable Long applicationId,
                                                                      Authentication authentication) {
        LoanApplicationResponse response = managerLoanService.getApplicationById(applicationId, authentication);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/loans/{applicationId}/approve")
    @Operation(summary = "Approve a loan application", description = "Sets approved amount, interest rate, tenure, and remarks. Sets status to APPROVED")
    public ResponseEntity<LoanApplicationResponse> approveApplication(@PathVariable Long applicationId,
                                                                      @Valid @RequestBody ManagerDecisionRequest request,
                                                                      Authentication authentication) {
        LoanApplicationResponse response = managerLoanService.approveApplication(applicationId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/loans/{applicationId}/reject")
    @Operation(summary = "Reject a loan application", description = "Rejects the application. Rejection reason is required")
    public ResponseEntity<LoanApplicationResponse> rejectApplication(@PathVariable Long applicationId,
                                                                      @Valid @RequestBody ManagerDecisionRequest request,
                                                                      Authentication authentication) {
        LoanApplicationResponse response = managerLoanService.rejectApplication(applicationId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/loans/{applicationId}/disburse")
    @Operation(summary = "Disburse an approved loan", description = "Simulates loan fund credit and generates monthly reducing-balance EMI schedule")
    public ResponseEntity<DisbursementResponse> disburseLoan(@PathVariable Long applicationId,
                                                             Authentication authentication) {
        DisbursementResponse response = managerLoanService.disburseLoan(applicationId, authentication);
        return ResponseEntity.ok(response);
    }
}

