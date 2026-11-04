package com.bankcore360.controller;

import com.bankcore360.dto.KycDocumentResponse;
import com.bankcore360.dto.KycDocumentReviewRequest;
import com.bankcore360.entity.KycDocumentType;
import com.bankcore360.service.KycDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class KycDocumentController {

    private final KycDocumentService kycDocumentService;

    public KycDocumentController(KycDocumentService kycDocumentService) {
        this.kycDocumentService = kycDocumentService;
    }

    // Customer APIs

    @PostMapping(value = "/api/customers/{customerId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KycDocumentResponse uploadDocument(
            @PathVariable Long customerId,
            @RequestParam("documentType") KycDocumentType documentType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return kycDocumentService.uploadDocument(customerId, documentType, file, authentication);
    }

    @GetMapping("/api/customers/{customerId}/documents")
    public List<KycDocumentResponse> getCustomerDocuments(
            @PathVariable Long customerId,
            Authentication authentication
    ) {
        return kycDocumentService.getDocumentsForCustomer(customerId, authentication);
    }

    @GetMapping("/api/customers/{customerId}/documents/{documentId}")
    public KycDocumentResponse getCustomerDocumentById(
            @PathVariable Long customerId,
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return kycDocumentService.getDocumentById(customerId, documentId, authentication);
    }

    // Checker APIs

    @GetMapping("/api/kyc/documents/pending")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentService.getPendingDocuments();
    }

    @GetMapping("/api/kyc/documents/{documentId}")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    public KycDocumentResponse getDocumentByIdForReview(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return kycDocumentService.getDocumentById(documentId, authentication);
    }

    @PatchMapping("/api/kyc/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('CHECKER', 'MANAGER', 'ADMIN')")
    public KycDocumentResponse reviewDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody KycDocumentReviewRequest request,
            Authentication authentication
    ) {
        return kycDocumentService.reviewDocument(documentId, request, authentication);
    }
}

