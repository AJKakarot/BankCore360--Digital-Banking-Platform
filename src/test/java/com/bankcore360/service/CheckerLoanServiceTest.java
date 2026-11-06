package com.bankcore360.service;

import com.bankcore360.dto.DocumentReviewRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InvalidDocumentException;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.exception.LoanDocumentNotFoundException;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import com.bankcore360.repository.LoanDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckerLoanServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanDocumentRepository loanDocumentRepository;
    @Mock
    private LoanDocumentStorageService documentStorageService;
    @Mock
    private LoanAuditService loanAuditService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CheckerLoanService checkerLoanService;

    private Customer customer;
    private LoanApplication application;
    private LoanDocument panDocument;
    private LoanDocument incomeDocument;
    private Authentication checkerAuth;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("John Doe");
        customer.setEmail("john.doe@bankcore360.com");

        application = new LoanApplication("LN-2026-CHK01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Personal expenses");
        application.setId(10L);

        panDocument = new LoanDocument(application, LoanDocumentType.PAN, "pan_ref.pdf", "pan.pdf", 1024L, "application/pdf");
        panDocument.setId(101L);

        incomeDocument = new LoanDocument(application, LoanDocumentType.INCOME_PROOF, "income_ref.pdf", "salary.pdf", 2048L, "application/pdf");
        incomeDocument.setId(102L);

        checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
    }

    @Test
    @DisplayName("Should auto-transition SUBMITTED application to UNDER_REVIEW when checker inspects application")
    void shouldAutoTransitionToUnderReview() {
        application.setStatus(LoanStatus.SUBMITTED);
        when(loanApplicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationResponse response = checkerLoanService.getApplicationById(10L, checkerAuth);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        verify(loanAuditService).logAction(
                eq("LN-2026-CHK01"),
                eq("dev_checker"),
                eq("ROLE_CHECKER"),
                eq("LOAN_STATUS_UPDATED"),
                eq("SUBMITTED"),
                eq("UNDER_REVIEW"),
                any()
        );
    }

    @Test
    @DisplayName("Should successfully verify document, log audit, and notify applicant")
    void shouldVerifyDocumentSuccessfully() {
        when(loanDocumentRepository.findById(101L)).thenReturn(Optional.of(panDocument));
        when(loanDocumentRepository.save(any(LoanDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentReviewRequest req = new DocumentReviewRequest("Document matches requirements", null);
        LoanDocumentResponse response = checkerLoanService.verifyDocument(101L, req, checkerAuth);

        assertThat(response.getDocumentStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        assertThat(response.getReviewedBy()).isEqualTo("dev_checker");

        verify(loanAuditService).logAction(
                eq("LN-2026-CHK01"),
                eq("dev_checker"),
                eq("ROLE_CHECKER"),
                eq("DOCUMENT_VERIFIED"),
                any(),
                eq("VERIFIED"),
                any()
        );
        verify(notificationService).notifyUser(eq("john.doe@bankcore360.com"), contains("Verified"), any(), eq("LN-2026-CHK01"));
    }

    @Test
    @DisplayName("Should reject document with mandatory reason, log audit, and notify applicant")
    void shouldRejectDocumentSuccessfully() {
        when(loanDocumentRepository.findById(101L)).thenReturn(Optional.of(panDocument));
        when(loanDocumentRepository.save(any(LoanDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentReviewRequest req = new DocumentReviewRequest(null, "Photo is blurry and PAN number is unreadable");
        LoanDocumentResponse response = checkerLoanService.rejectDocument(101L, req, checkerAuth);

        assertThat(response.getDocumentStatus()).isEqualTo(DocumentVerificationStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Photo is blurry and PAN number is unreadable");

        verify(loanAuditService).logAction(
                eq("LN-2026-CHK01"),
                eq("dev_checker"),
                eq("ROLE_CHECKER"),
                eq("DOCUMENT_REJECTED"),
                any(),
                eq("REJECTED"),
                contains("blurry")
        );
        verify(notificationService).notifyUser(eq("john.doe@bankcore360.com"), contains("Rejected"), any(), eq("LN-2026-CHK01"));
    }

    @Test
    @DisplayName("Should throw exception when rejecting document without rejection reason")
    void shouldFailRejectionWithoutReason() {
        when(loanDocumentRepository.findById(101L)).thenReturn(Optional.of(panDocument));

        DocumentReviewRequest req = new DocumentReviewRequest("Rejected", null);

        assertThatThrownBy(() -> checkerLoanService.rejectDocument(101L, req, checkerAuth))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Rejection reason is mandatory");
    }

    @Test
    @DisplayName("Should prevent reviewer from reviewing their own loan application")
    void shouldPreventSelfReview() {
        Authentication selfAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_CHECKER");
        when(loanDocumentRepository.findById(101L)).thenReturn(Optional.of(panDocument));

        DocumentReviewRequest req = new DocumentReviewRequest("Self verify", null);

        assertThatThrownBy(() -> checkerLoanService.verifyDocument(101L, req, selfAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Reviewers are not permitted to inspect or verify their own loan applications");
    }

    @Test
    @DisplayName("Should forward application to Manager when all documents are VERIFIED")
    void shouldForwardToManagerWhenAllDocumentsVerified() {
        panDocument.setDocumentStatus(DocumentVerificationStatus.VERIFIED);
        incomeDocument.setDocumentStatus(DocumentVerificationStatus.VERIFIED);

        when(loanApplicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(10L))
                .thenReturn(List.of(panDocument, incomeDocument));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationResponse response = checkerLoanService.forwardToManager(10L, "Documents look great", checkerAuth);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.PENDING_MANAGER_APPROVAL);
        verify(loanAuditService).logAction(
                eq("LN-2026-CHK01"),
                eq("dev_checker"),
                eq("ROLE_CHECKER"),
                eq("FORWARDED_TO_MANAGER"),
                any(),
                eq("PENDING_MANAGER_APPROVAL"),
                any()
        );
    }

    @Test
    @DisplayName("Should reject forwarding to Manager if any document is still PENDING")
    void shouldRejectForwardingWhenDocsPending() {
        panDocument.setDocumentStatus(DocumentVerificationStatus.VERIFIED);
        incomeDocument.setDocumentStatus(DocumentVerificationStatus.PENDING); // Pending!

        when(loanApplicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(10L))
                .thenReturn(List.of(panDocument, incomeDocument));

        assertThatThrownBy(() -> checkerLoanService.forwardToManager(10L, "Ready", checkerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("all documents must be verified first. Some documents are still PENDING");
    }

    @Test
    @DisplayName("Should reject forwarding to Manager if any document is REJECTED")
    void shouldRejectForwardingWhenDocsRejected() {
        panDocument.setDocumentStatus(DocumentVerificationStatus.VERIFIED);
        incomeDocument.setDocumentStatus(DocumentVerificationStatus.REJECTED); // Rejected!

        when(loanApplicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(10L))
                .thenReturn(List.of(panDocument, incomeDocument));

        assertThatThrownBy(() -> checkerLoanService.forwardToManager(10L, "Ready", checkerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("contains REJECTED documents");
    }
}

