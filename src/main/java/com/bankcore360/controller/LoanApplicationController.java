package com.bankcore360.controller;

import com.bankcore360.dto.*;
import com.bankcore360.entity.LoanDocumentType;
import com.bankcore360.service.CustomerLoanService;
import com.bankcore360.service.LoanEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loan Applications", description = "Endpoints for customer loan applications, documents, eligibility calculation, and repayment tracking")
public class LoanApplicationController {

    private final CustomerLoanService customerLoanService;
    private final LoanEligibilityService loanEligibilityService;

    public LoanApplicationController(CustomerLoanService customerLoanService,
                                     LoanEligibilityService loanEligibilityService) {
        this.customerLoanService = customerLoanService;
        this.loanEligibilityService = loanEligibilityService;
    }

    @PostMapping("/calculator")
    @Operation(summary = "Calculate loan eligibility and monthly reducing-balance EMI", description = "Public or authenticated endpoint to simulate EMI obligations and borrowing capacity")
    public ResponseEntity<LoanEligibilityResponse> calculateEligibility(@Valid @RequestBody LoanEligibilityRequest request) {
        LoanEligibilityResponse response = loanEligibilityService.calculateEligibility(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Submit a new loan application", description = "Authenticated CUSTOMER or ADMIN role can submit loan applications")
    public ResponseEntity<LoanApplicationResponse> applyLoan(@Valid @RequestBody LoanApplicationRequest request,
                                                             Authentication authentication) {
        LoanApplicationResponse response = customerLoanService.applyLoan(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "View my loan applications", description = "Returns all loan applications submitted by the authenticated customer")
    public ResponseEntity<List<LoanApplicationResponse>> getMyApplications(Authentication authentication) {
        List<LoanApplicationResponse> applications = customerLoanService.getMyApplications(authentication);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{applicationReference}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get loan application details by reference", description = "Customers can view only their own application; Staff can view any application")
    public ResponseEntity<LoanApplicationResponse> getApplicationByReference(@PathVariable String applicationReference,
                                                                             Authentication authentication) {
        LoanApplicationResponse response = customerLoanService.getApplicationByReference(applicationReference, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{applicationReference}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Upload supporting document for a loan application", description = "Customer uploads required document (PAN, Aadhaar, Income Proof, Bank Statement, etc.)")
    public ResponseEntity<LoanDocumentResponse> uploadDocument(@PathVariable String applicationReference,
                                                               @RequestParam("documentType") LoanDocumentType documentType,
                                                               @RequestParam("file") MultipartFile file,
                                                               Authentication authentication) {
        LoanDocumentResponse response = customerLoanService.uploadDocument(applicationReference, documentType, file, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{applicationReference}/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "List documents attached to a loan application", description = "Returns metadata and verification status for all uploaded documents")
    public ResponseEntity<List<LoanDocumentResponse>> getDocumentsForApplication(@PathVariable String applicationReference,
                                                                                 Authentication authentication) {
        List<LoanDocumentResponse> documents = customerLoanService.getDocumentsForApplication(applicationReference, authentication);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{applicationReference}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Download a document file", description = "Securely streams the document file from private storage")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String applicationReference,
                                                     @PathVariable Long documentId,
                                                     Authentication authentication) {
        Resource fileResource = customerLoanService.downloadDocument(applicationReference, documentId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    @GetMapping("/my-repayments")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "View all repayments for customer", description = "Returns all upcoming, overdue, and paid EMI installments across all customer loans")
    public ResponseEntity<List<LoanRepaymentResponse>> getMyRepayments(Authentication authentication) {
        List<LoanRepaymentResponse> repayments = customerLoanService.getCustomerRepayments(authentication);
        return ResponseEntity.ok(repayments);
    }

    @PostMapping("/repayments/{repaymentId}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "1-Click pay EMI installment", description = "Directly pays an EMI installment by its ID, applying any late fees and crediting Bank Vault")
    public ResponseEntity<LoanRepaymentResponse> payRepaymentDirect(@PathVariable Long repaymentId,
                                                                    Authentication authentication) {
        LoanRepaymentResponse response = customerLoanService.payRepaymentById(repaymentId, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/repayments/trigger-overdue-and-reminders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Trigger overdue penalties and EMI reminder notifications", description = "Scans all unpaid installments, adds ₹500 late penalty to overdue EMIs, and dispatches actionable notifications")
    public ResponseEntity<String> triggerOverdueAndReminders() {
        int count = customerLoanService.triggerMonthlyOverdueAndReminders();
        return ResponseEntity.ok("Processed " + count + " monthly overdue/reminder repayment actions.");
    }

    @GetMapping("/{applicationReference}/repayments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CHECKER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "View EMI repayment schedule", description = "Returns monthly amortization schedule for disbursed loans")
    public ResponseEntity<List<LoanRepaymentResponse>> getRepaymentSchedule(@PathVariable String applicationReference,
                                                                            Authentication authentication) {
        List<LoanRepaymentResponse> schedule = customerLoanService.getRepaymentSchedule(applicationReference, authentication);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{applicationReference}/repayments/{repaymentId}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Simulated payment for an EMI installment", description = "Records repayment of an installment and updates outstanding balance")
    public ResponseEntity<LoanRepaymentResponse> payEmiInstallment(@PathVariable String applicationReference,
                                                                   @PathVariable Long repaymentId,
                                                                   Authentication authentication) {
        LoanRepaymentResponse response = customerLoanService.payEmiInstallment(applicationReference, repaymentId, authentication);
        return ResponseEntity.ok(response);
    }
}

