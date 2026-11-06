package com.bankcore360.service;

import com.bankcore360.dto.KycDocumentResponse;
import com.bankcore360.dto.KycDocumentReviewRequest;
import com.bankcore360.entity.*;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.exception.DocumentNotFoundException;
import com.bankcore360.exception.InvalidDocumentException;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.KycDocumentAuditLogRepository;
import com.bankcore360.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycDocumentServiceTest {

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private KycDocumentAuditLogRepository auditLogRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private KycDocumentService kycDocumentService;

    private Customer sampleCustomer;
    private Authentication customerAuth;
    private Authentication checkerAuth;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setFullName("John Doe");
        sampleCustomer.setEmail("john.doe@bankcore360.com");
        sampleCustomer.setKycStatus(KycStatus.PENDING);

        customerAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_CUSTOMER");
        checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
    }

    @Test
    @DisplayName("Should successfully upload KYC document when customer owns the resource")
    void shouldUploadDocumentSuccessfully() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar.pdf", "application/pdf", "dummy pdf content".getBytes()
        );
        when(fileStorageService.storeFile(file, 1L, KycDocumentType.AADHAAR)).thenReturn("cust_1_aadhaar_uuid.pdf");

        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(inv -> {
            KycDocument doc = inv.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(10L);
            }
            return doc;
        });

        KycDocumentResponse response = kycDocumentService.uploadDocument(1L, KycDocumentType.AADHAAR, file, customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getDocumentType()).isEqualTo(KycDocumentType.AADHAAR);
        assertThat(response.getVerificationStatus()).isEqualTo(DocumentVerificationStatus.PENDING);
        assertThat(response.getFileName()).isEqualTo("aadhaar.pdf");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when customer tries to upload document to another customer's ID")
    void shouldForbidCustomerFromUploadingToAnotherCustomerId() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(sampleCustomer)); // sampleCustomer is john.doe@bankcore360.com
        Authentication otherCustomerAuth = new TestingAuthenticationToken("attacker", null, "ROLE_CUSTOMER");
        when(appUserRepository.findByUsername("attacker")).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> kycDocumentService.uploadDocument(2L, KycDocumentType.AADHAAR, file, otherCustomerAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view or modify documents for another customer");

        verify(fileStorageService, never()).storeFile(any(), any(), any());
    }

    @Test
    @DisplayName("Should allow customer to view own uploaded documents")
    void shouldAllowCustomerToViewOwnDocuments() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        KycDocument doc = new KycDocument(1L, KycDocumentType.AADHAAR, "path", "aadhaar.pdf", 1024L, "application/pdf");
        doc.setId(10L);
        when(kycDocumentRepository.findByCustomerIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(doc));

        List<KycDocumentResponse> docs = kycDocumentService.getDocumentsForCustomer(1L, customerAuth);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getId()).isEqualTo(10L);
        assertThat(docs.get(0).getDocumentType()).isEqualTo(KycDocumentType.AADHAAR);
    }

    @Test
    @DisplayName("Should forbid customer from viewing another customer's uploaded documents")
    void shouldForbidCustomerFromViewingAnotherCustomerDocuments() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        Authentication otherCustAuth = new TestingAuthenticationToken("intruder", null, "ROLE_CUSTOMER");
        when(appUserRepository.findByUsername("intruder")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycDocumentService.getDocumentsForCustomer(1L, otherCustAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view or modify documents for another customer");
    }

    @Test
    @DisplayName("Should return pending documents for reviewer")
    void shouldReturnPendingDocumentsForReviewer() {
        KycDocument pendingDoc = new KycDocument(1L, KycDocumentType.PAN, "path", "pan.png", 500L, "image/png");
        pendingDoc.setId(11L);
        when(kycDocumentRepository.findByVerificationStatusOrderByUploadedAtAsc(DocumentVerificationStatus.PENDING))
                .thenReturn(List.of(pendingDoc));

        List<KycDocumentResponse> pending = kycDocumentService.getPendingDocuments();

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(11L);
        assertThat(pending.get(0).getVerificationStatus()).isEqualTo(DocumentVerificationStatus.PENDING);
    }

    @Test
    @DisplayName("Should allow checker to verify document successfully and record audit log")
    void shouldAllowCheckerToVerifyDocument() {
        KycDocument doc = new KycDocument(1L, KycDocumentType.AADHAAR, "path", "aadhaar.pdf", 1024L, "application/pdf");
        doc.setId(10L);
        when(kycDocumentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        KycDocumentResponse response = kycDocumentService.reviewDocument(10L, request, checkerAuth);

        assertThat(response.getVerificationStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(response.getReviewedBy()).isEqualTo("dev_checker");
        assertThat(response.getReviewedAt()).isNotNull();

        ArgumentCaptor<KycDocumentAuditLog> captor = ArgumentCaptor.forClass(KycDocumentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        KycDocumentAuditLog audit = captor.getValue();
        assertThat(audit.getDocumentId()).isEqualTo(10L);
        assertThat(audit.getPreviousStatus()).isEqualTo(DocumentVerificationStatus.PENDING);
        assertThat(audit.getNewStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(audit.getReviewerUsername()).isEqualTo("dev_checker");
    }

    @Test
    @DisplayName("Should allow checker to reject document with mandatory rejection reason")
    void shouldAllowCheckerToRejectDocumentWithReason() {
        KycDocument doc = new KycDocument(1L, KycDocumentType.PAN, "path", "pan.jpg", 1024L, "image/jpeg");
        doc.setId(12L);
        when(kycDocumentRepository.findById(12L)).thenReturn(Optional.of(doc));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        KycDocumentReviewRequest request = new KycDocumentReviewRequest(
                DocumentVerificationStatus.REJECTED, "Document is blurred and unreadable"
        );

        KycDocumentResponse response = kycDocumentService.reviewDocument(12L, request, checkerAuth);

        assertThat(response.getVerificationStatus()).isEqualTo(DocumentVerificationStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Document is blurred and unreadable");
        assertThat(response.getReviewedBy()).isEqualTo("dev_checker");

        verify(auditLogRepository).save(any(KycDocumentAuditLog.class));
    }

    @Test
    @DisplayName("Should throw InvalidDocumentException when rejecting document without rejection reason")
    void shouldThrowWhenRejectingWithoutReason() {
        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.REJECTED, "");

        assertThatThrownBy(() -> kycDocumentService.reviewDocument(10L, request, checkerAuth))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Rejection reason is mandatory when rejecting a document");

        verify(kycDocumentRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent self-review when reviewer is the customer who owns the document")
    void shouldPreventSelfReviewWhenReviewerIsCustomer() {
        KycDocument doc = new KycDocument(1L, KycDocumentType.AADHAAR, "path", "aadhaar.pdf", 1024L, "application/pdf");
        doc.setId(10L);
        when(kycDocumentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer)); // email is john.doe@bankcore360.com

        Authentication selfReviewerAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_CHECKER");

        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        assertThatThrownBy(() -> kycDocumentService.reviewDocument(10L, request, selfReviewerAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Reviewers are not permitted to review or verify their own KYC documents");

        verify(kycDocumentRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DocumentNotFoundException when document does not exist")
    void shouldThrowWhenDocumentNotFound() {
        when(kycDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        KycDocumentReviewRequest request = new KycDocumentReviewRequest(DocumentVerificationStatus.VERIFIED, null);

        assertThatThrownBy(() -> kycDocumentService.reviewDocument(999L, request, checkerAuth))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("Document with ID 999 not found");
    }
}

