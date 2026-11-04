package com.bankcore360.controller;

import com.bankcore360.dto.DocumentReviewRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.service.CheckerLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checker")
@Tag(name = "Checker Workflow", description = "Endpoints for document inspection, verification, rejection, and forwarding to Manager")
@PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
public class CheckerLoanController {

    private final CheckerLoanService checkerLoanService;

    public CheckerLoanController(CheckerLoanService checkerLoanService) {
        this.checkerLoanService = checkerLoanService;
    }

    @GetMapping("/loans/pending")
    @Operation(summary = "Get pending loan applications for checker review", description = "Returns applications in SUBMITTED or UNDER_REVIEW state")
    public ResponseEntity<List<LoanApplicationResponse>> getPendingApplications() {
        List<LoanApplicationResponse> applications = checkerLoanService.getPendingApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/loans/{applicationId}")
    @Operation(summary = "Inspect a loan application", description = "Returns loan application details and transitions status from SUBMITTED to UNDER_REVIEW")
    public ResponseEntity<LoanApplicationResponse> getApplicationById(@PathVariable Long applicationId,
                                                                      Authentication authentication) {
        LoanApplicationResponse response = checkerLoanService.getApplicationById(applicationId, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/loans/{applicationId}/documents")
    @Operation(summary = "Get documents for an application", description = "Lists all supporting documents attached to the application")
    public ResponseEntity<List<LoanDocumentResponse>> getDocumentsForApplication(@PathVariable Long applicationId,
                                                                                 Authentication authentication) {
        List<LoanDocumentResponse> documents = checkerLoanService.getDocumentsForApplication(applicationId, authentication);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{documentId}/file")
    @Operation(summary = "Download or stream document for inspection", description = "Streams document file from private storage for verification")
    public ResponseEntity<Resource> downloadDocumentFile(@PathVariable Long documentId,
                                                         Authentication authentication) {
        Resource fileResource = checkerLoanService.downloadDocumentFile(documentId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    @PatchMapping("/documents/{documentId}/verify")
    @Operation(summary = "Verify a document", description = "Marks document as VERIFIED with optional remarks")
    public ResponseEntity<LoanDocumentResponse> verifyDocument(@PathVariable Long documentId,
                                                               @RequestBody(required = false) DocumentReviewRequest request,
                                                               Authentication authentication) {
        LoanDocumentResponse response = checkerLoanService.verifyDocument(documentId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/documents/{documentId}/reject")
    @Operation(summary = "Reject a document", description = "Marks document as REJECTED. Rejection reason is mandatory")
    public ResponseEntity<LoanDocumentResponse> rejectDocument(@PathVariable Long documentId,
                                                               @Valid @RequestBody DocumentReviewRequest request,
                                                               Authentication authentication) {
        LoanDocumentResponse response = checkerLoanService.rejectDocument(documentId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/loans/{applicationId}/forward")
    @Operation(summary = "Forward application to Manager", description = "Ensures all documents are VERIFIED, then transitions to PENDING_MANAGER_APPROVAL")
    public ResponseEntity<LoanApplicationResponse> forwardToManager(@PathVariable Long applicationId,
                                                                    @RequestParam(value = "remarks", required = false) String remarks,
                                                                    Authentication authentication) {
        LoanApplicationResponse response = checkerLoanService.forwardToManager(applicationId, remarks, authentication);
        return ResponseEntity.ok(response);
    }
}

