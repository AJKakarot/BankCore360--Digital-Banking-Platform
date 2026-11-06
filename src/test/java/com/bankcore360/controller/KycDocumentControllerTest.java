package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.KycDocumentResponse;
import com.bankcore360.dto.KycDocumentReviewRequest;
import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.KycDocumentType;
import com.bankcore360.exception.DocumentNotFoundException;
import com.bankcore360.exception.InvalidDocumentException;
import com.bankcore360.service.KycDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KycDocumentController.class)
@Import(SecurityConfig.class)
class KycDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KycDocumentService kycDocumentService;

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/customers/{customerId}/documents should return 201 when upload succeeds")
    void shouldUploadDocumentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes()
        );

        KycDocumentResponse response = new KycDocumentResponse(
                10L, 1L, KycDocumentType.AADHAAR, "/api/customers/1/documents/10/file",
                "aadhaar.pdf", 1024L, MediaType.APPLICATION_PDF_VALUE,
                DocumentVerificationStatus.PENDING, null, LocalDateTime.now(), null, null
        );

        when(kycDocumentService.uploadDocument(eq(1L), eq(KycDocumentType.AADHAAR), any(), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/customers/1/documents")
                        .file(file)
                        .param("documentType", "AADHAAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.documentType").value("AADHAAR"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "attacker_cust", roles = {"CUSTOMER"})
    @DisplayName("POST /api/customers/{customerId}/documents when customer attempts upload for another customer should return 403")
    void shouldForbidCustomerFromUploadingForAnotherCustomer() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes()
        );

        when(kycDocumentService.uploadDocument(eq(2L), eq(KycDocumentType.AADHAAR), any(), any()))
                .thenThrow(new AccessDeniedException("Access denied: You are not authorized to view or modify documents for another customer"));

        mockMvc.perform(multipart("/api/customers/2/documents")
                        .file(file)
                        .param("documentType", "AADHAAR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /api/customers/{customerId}/documents without authentication should return 401")
    void shouldReturnUnauthorizedWhenAnonymousUploads() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/customers/1/documents")
                        .file(file)
                        .param("documentType", "AADHAAR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/customers/{customerId}/documents should return 200 and list of uploaded documents")
    void shouldReturnCustomerUploadedDocuments() throws Exception {
        KycDocumentResponse doc = new KycDocumentResponse(
                10L, 1L, KycDocumentType.AADHAAR, "path", "aadhaar.pdf", 1024L, "application/pdf",
                DocumentVerificationStatus.PENDING, null, LocalDateTime.now(), null, null
        );

        when(kycDocumentService.getDocumentsForCustomer(eq(1L), any())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/customers/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].documentType").value("AADHAAR"));
    }

    @Test
    @WithMockUser(username = "attacker_cust", roles = {"CUSTOMER"})
    @DisplayName("GET /api/customers/{customerId}/documents for another customer should return 403 Forbidden")
    void shouldForbidCustomerFromViewingAnotherCustomerDocuments() throws Exception {
        when(kycDocumentService.getDocumentsForCustomer(eq(2L), any()))
                .thenThrow(new AccessDeniedException("Access denied: You are not authorized to view or modify documents for another customer"));

        mockMvc.perform(get("/api/customers/2/documents"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/kyc/documents/pending with CHECKER role should return 200 and pending documents")
    void shouldAllowCheckerToViewPendingDocuments() throws Exception {
        KycDocumentResponse pending = new KycDocumentResponse(
                10L, 1L, KycDocumentType.PAN, "path", "pan.png", 500L, "image/png",
                DocumentVerificationStatus.PENDING, null, LocalDateTime.now(), null, null
        );

        when(kycDocumentService.getPendingDocuments()).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/kyc/documents/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].verificationStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/kyc/documents/pending with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromViewingPendingDocuments() throws Exception {
        mockMvc.perform(get("/api/kyc/documents/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /api/kyc/documents/pending without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenAnonymousViewsPendingDocuments() throws Exception {
        mockMvc.perform(get("/api/kyc/documents/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify with VERIFIED status should return 200")
    void shouldAllowCheckerToVerifyDocument() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);
        KycDocumentResponse response = new KycDocumentResponse(
                10L, 1L, KycDocumentType.AADHAAR, "path", "aadhaar.pdf", 1024L, "application/pdf",
                DocumentVerificationStatus.VERIFIED, null, LocalDateTime.now(), LocalDateTime.now(), "dev_checker"
        );

        when(kycDocumentService.reviewDocument(eq(10L), any(KycDocumentReviewRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/kyc/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.reviewedBy").value("dev_checker"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify with REJECTED status and reason should return 200")
    void shouldAllowCheckerToRejectDocumentWithReason() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(
                DocumentVerificationStatus.REJECTED, "Document edges are cut off"
        );
        KycDocumentResponse response = new KycDocumentResponse(
                10L, 1L, KycDocumentType.PAN, "path", "pan.png", 500L, "image/png",
                DocumentVerificationStatus.REJECTED, "Document edges are cut off", LocalDateTime.now(), LocalDateTime.now(), "dev_checker"
        );

        when(kycDocumentService.reviewDocument(eq(10L), any(KycDocumentReviewRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/kyc/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Document edges are cut off"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify with REJECTED status without reason should return 400")
    void shouldReturnBadRequestWhenRejectingWithoutReason() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.REJECTED, "");

        when(kycDocumentService.reviewDocument(eq(10L), any(KycDocumentReviewRequest.class), any()))
                .thenThrow(new InvalidDocumentException("Rejection reason is mandatory when rejecting a document"));

        mockMvc.perform(patch("/api/kyc/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Rejection reason is mandatory when rejecting a document"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify when reviewer attempts self-review should return 403")
    void shouldReturnForbiddenWhenReviewerAttemptsSelfReview() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        when(kycDocumentService.reviewDocument(eq(10L), any(KycDocumentReviewRequest.class), any()))
                .thenThrow(new AccessDeniedException("Reviewers are not permitted to review or verify their own KYC documents"));

        mockMvc.perform(patch("/api/kyc/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Reviewers are not permitted to review or verify their own KYC documents"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify with CUSTOMER role should return 403 Forbidden")
    void shouldForbidCustomerFromReviewingDocument() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        mockMvc.perform(patch("/api/kyc/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/kyc/documents/{documentId}/verify when document does not exist should return 404")
    void shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        when(kycDocumentService.reviewDocument(eq(999L), any(KycDocumentReviewRequest.class), any()))
                .thenThrow(new DocumentNotFoundException("Document with ID 999 not found"));

        mockMvc.perform(patch("/api/kyc/documents/999/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Document with ID 999 not found"));
    }
}

