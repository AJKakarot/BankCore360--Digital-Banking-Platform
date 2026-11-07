package com.bankcore360.service;

import com.bankcore360.dto.DocumentReviewRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.entity.*;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.event.payload.KycDocumentRejectedPayload;
import com.bankcore360.event.payload.KycDocumentVerifiedPayload;
import com.bankcore360.exception.InvalidDocumentException;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.exception.LoanApplicationNotFoundException;
import com.bankcore360.exception.LoanDocumentNotFoundException;
import com.bankcore360.outbox.OutboxService;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import com.bankcore360.repository.LoanDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CheckerLoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanDocumentStorageService documentStorageService;
    private final LoanAuditService loanAuditService;
    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;
    private final OutboxService outboxService;

    private static final String KYC_EVENTS_TOPIC = "kyc-events";

    @Autowired
    public CheckerLoanService(LoanApplicationRepository loanApplicationRepository,
                              LoanDocumentRepository loanDocumentRepository,
                              LoanDocumentStorageService documentStorageService,
                              LoanAuditService loanAuditService,
                              NotificationService notificationService,
                              AppUserRepository appUserRepository,
                              OutboxService outboxService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanDocumentRepository = loanDocumentRepository;
        this.documentStorageService = documentStorageService;
        this.loanAuditService = loanAuditService;
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
        this.outboxService = outboxService;
    }

    public CheckerLoanService(LoanApplicationRepository loanApplicationRepository,
                              LoanDocumentRepository loanDocumentRepository,
                              LoanDocumentStorageService documentStorageService,
                              LoanAuditService loanAuditService,
                              NotificationService notificationService,
                              AppUserRepository appUserRepository) {
        this(loanApplicationRepository, loanDocumentRepository, documentStorageService, loanAuditService, notificationService, appUserRepository, null);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getPendingApplications() {
        return loanApplicationRepository.findByStatusInOrderBySubmittedAtAsc(
                List.of(LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW)
        ).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public LoanApplicationResponse getApplicationById(Long applicationId, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        // Automatically transition from SUBMITTED to UNDER_REVIEW on first checker inspection
        if (app.getStatus() == LoanStatus.SUBMITTED) {
            app.setStatus(LoanStatus.UNDER_REVIEW);
            LoanApplication updated = loanApplicationRepository.save(app);
            String actor = auth != null ? auth.getName() : "CHECKER";
            loanAuditService.logAction(
                    app.getApplicationReference(),
                    actor,
                    "ROLE_CHECKER",
                    "LOAN_STATUS_UPDATED",
                    LoanStatus.SUBMITTED.name(),
                    LoanStatus.UNDER_REVIEW.name(),
                    "Checker initiated application review"
            );
            return mapToResponse(updated);
        }

        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponse> getDocumentsForApplication(Long applicationId, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        return loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(applicationId)
                .stream()
                .map(this::mapToDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(Long documentId, Authentication auth) {
        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new LoanDocumentNotFoundException("Loan document with ID " + documentId + " not found"));

        validateNotSelfReview(auth, doc.getLoanApplication().getCustomer());

        return documentStorageService.loadFileAsResource(doc.getStorageReference());
    }

    @Transactional
    public LoanDocumentResponse verifyDocument(Long documentId, DocumentReviewRequest request, Authentication auth) {
        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new LoanDocumentNotFoundException("Loan document with ID " + documentId + " not found"));

        LoanApplication app = doc.getLoanApplication();
        validateNotSelfReview(auth, app.getCustomer());

        DocumentVerificationStatus prevStatus = doc.getDocumentStatus();
        doc.setDocumentStatus(DocumentVerificationStatus.VERIFIED);
        doc.setRejectionReason(null);
        if (request != null && request.getRemarks() != null) {
            doc.setRemarks(request.getRemarks());
        }
        doc.setReviewedAt(LocalDateTime.now());
        String reviewer = auth != null ? auth.getName() : "CHECKER";
        doc.setReviewedBy(reviewer);

        LoanDocument savedDoc = loanDocumentRepository.save(doc);

        loanAuditService.logAction(
                app.getApplicationReference(),
                reviewer,
                "ROLE_CHECKER",
                "DOCUMENT_VERIFIED",
                prevStatus != null ? prevStatus.name() : null,
                DocumentVerificationStatus.VERIFIED.name(),
                "Verified document: " + doc.getDocumentType()
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Document Verified",
                "Your document " + doc.getDocumentType() + " for application "
                        + app.getApplicationReference() + " was verified by reviewer.",
                app.getApplicationReference()
        );

        if (outboxService != null) {
            KycDocumentVerifiedPayload payload = new KycDocumentVerifiedPayload(
                    savedDoc.getId(),
                    app.getApplicationReference(),
                    app.getCustomer().getId(),
                    savedDoc.getDocumentType(),
                    reviewer,
                    savedDoc.getReviewedAt()
            );
            outboxService.saveEvent(KYC_EVENTS_TOPIC, EventEnvelope.create("KYC_DOCUMENT_VERIFIED", app.getApplicationReference(), payload));
        }

        // If all documents for this application are now verified, transition status to DOCUMENTS_VERIFIED
        List<LoanDocument> allDocs = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(app.getId());
        if (!allDocs.isEmpty() && allDocs.stream().allMatch(d -> d.getDocumentStatus() == DocumentVerificationStatus.VERIFIED)) {
            if (app.getStatus() == LoanStatus.SUBMITTED || app.getStatus() == LoanStatus.UNDER_REVIEW) {
                LoanStatus previousAppStatus = app.getStatus();
                app.setStatus(LoanStatus.DOCUMENTS_VERIFIED);
                loanApplicationRepository.save(app);
                loanAuditService.logAction(
                        app.getApplicationReference(),
                        reviewer,
                        "ROLE_CHECKER",
                        "DOCUMENTS_VERIFIED",
                        previousAppStatus.name(),
                        LoanStatus.DOCUMENTS_VERIFIED.name(),
                        "All documents verified by Checker. Application transitioned to DOCUMENTS_VERIFIED."
                );
            }
        }

        return mapToDocumentResponse(savedDoc);
    }

    @Transactional
    public LoanDocumentResponse rejectDocument(Long documentId, DocumentReviewRequest request, Authentication auth) {
        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new LoanDocumentNotFoundException("Loan document with ID " + documentId + " not found"));

        LoanApplication app = doc.getLoanApplication();
        validateNotSelfReview(auth, app.getCustomer());

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().isBlank()) {
            throw new InvalidDocumentException("Rejection reason is mandatory when rejecting a document");
        }

        DocumentVerificationStatus prevStatus = doc.getDocumentStatus();
        doc.setDocumentStatus(DocumentVerificationStatus.REJECTED);
        doc.setRejectionReason(request.getRejectionReason().trim());
        if (request.getRemarks() != null) {
            doc.setRemarks(request.getRemarks().trim());
        }
        doc.setReviewedAt(LocalDateTime.now());
        String reviewer = auth != null ? auth.getName() : "CHECKER";
        doc.setReviewedBy(reviewer);

        LoanDocument savedDoc = loanDocumentRepository.save(doc);

        loanAuditService.logAction(
                app.getApplicationReference(),
                reviewer,
                "ROLE_CHECKER",
                "DOCUMENT_REJECTED",
                prevStatus != null ? prevStatus.name() : null,
                DocumentVerificationStatus.REJECTED.name(),
                "Rejected document " + doc.getDocumentType() + ". Reason: " + request.getRejectionReason().trim()
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Document Rejected",
                "Your document " + doc.getDocumentType() + " for application "
                        + app.getApplicationReference() + " was rejected. Reason: " + request.getRejectionReason().trim(),
                app.getApplicationReference()
        );

        if (outboxService != null) {
            KycDocumentRejectedPayload payload = new KycDocumentRejectedPayload(
                    savedDoc.getId(),
                    app.getApplicationReference(),
                    app.getCustomer().getId(),
                    savedDoc.getDocumentType(),
                    request.getRejectionReason().trim(),
                    reviewer,
                    savedDoc.getReviewedAt()
            );
            outboxService.saveEvent(KYC_EVENTS_TOPIC, EventEnvelope.create("KYC_DOCUMENT_REJECTED", app.getApplicationReference(), payload));
        }

        return mapToDocumentResponse(savedDoc);
    }

    @Transactional
    public LoanApplicationResponse forwardToManager(Long applicationId, String checkerRemarks, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        if (app.getStatus() != LoanStatus.SUBMITTED && app.getStatus() != LoanStatus.UNDER_REVIEW && app.getStatus() != LoanStatus.DOCUMENTS_VERIFIED) {
            throw new InvalidLoanStateException("Application is in status " + app.getStatus()
                    + ". Only SUBMITTED, UNDER_REVIEW, or DOCUMENTS_VERIFIED applications can be forwarded to Manager.");
        }

        List<LoanDocument> docs = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(applicationId);
        if (docs.isEmpty()) {
            throw new InvalidLoanStateException("Cannot forward application to Manager: No supporting documents have been uploaded.");
        }

        boolean hasPending = docs.stream().anyMatch(d -> d.getDocumentStatus() == DocumentVerificationStatus.PENDING);
        if (hasPending) {
            throw new InvalidLoanStateException("Cannot forward application: all documents must be verified first. Some documents are still PENDING review.");
        }

        boolean hasRejected = docs.stream().anyMatch(d -> d.getDocumentStatus() == DocumentVerificationStatus.REJECTED);
        if (hasRejected) {
            throw new InvalidLoanStateException("Cannot forward application: contains REJECTED documents. The customer must resubmit and verification must complete.");
        }

        LoanStatus prevStatus = app.getStatus();
        app.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);
        LoanApplication savedApp = loanApplicationRepository.save(app);

        String reviewer = auth != null ? auth.getName() : "CHECKER";
        String remarks = (checkerRemarks != null && !checkerRemarks.isBlank())
                ? checkerRemarks
                : "All documents verified successfully by Checker";

        loanAuditService.logAction(
                app.getApplicationReference(),
                reviewer,
                "ROLE_CHECKER",
                "FORWARDED_TO_MANAGER",
                prevStatus.name(),
                LoanStatus.PENDING_MANAGER_APPROVAL.name(),
                remarks
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Application Forwarded to Manager",
                "All your documents have been verified for application "
                        + app.getApplicationReference() + ". Forwarded for Manager final decision.",
                app.getApplicationReference()
        );

        return mapToResponse(savedApp);
    }

    private void validateNotSelfReview(Authentication auth, Customer customer) {
        if (auth != null && isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Reviewers are not permitted to inspect or verify their own loan applications");
        }
    }

    private boolean isSamePerson(String principalName, String customerEmail) {
        if (principalName == null || customerEmail == null) {
            return false;
        }
        String p = principalName.trim().toLowerCase();
        String c = customerEmail.trim().toLowerCase();

        if (p.equals(c)) {
            return true;
        }

        int atIndex = c.indexOf('@');
        if (atIndex > 0) {
            String emailLocalPart = c.substring(0, atIndex);
            if (p.equals(emailLocalPart)) {
                return true;
            }
        }

        int pAtIndex = p.indexOf('@');
        if (pAtIndex > 0) {
            String principalLocalPart = p.substring(0, pAtIndex);
            if (principalLocalPart.equals(c)) {
                return true;
            }
            if (atIndex > 0 && principalLocalPart.equals(c.substring(0, atIndex))) {
                return true;
            }
        }

        Optional<AppUser> userOpt = appUserRepository.findByUsername(principalName);
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            return userOpt.get().getEmail().trim().equalsIgnoreCase(c);
        }

        return false;
    }

    private LoanApplicationResponse mapToResponse(LoanApplication app) {
        LoanApplicationResponse res = new LoanApplicationResponse();
        res.setId(app.getId());
        res.setApplicationReference(app.getApplicationReference());
        res.setCustomerId(app.getCustomer().getId());
        res.setCustomerFullName(app.getCustomer().getFullName());
        res.setCustomerEmail(app.getCustomer().getEmail());
        res.setLoanType(app.getLoanType());
        res.setRequestedAmount(app.getRequestedAmount());
        res.setTenureMonths(app.getTenureMonths());
        res.setPurpose(app.getPurpose());
        res.setStatus(app.getStatus());
        res.setSubmittedAt(app.getSubmittedAt());
        res.setUpdatedAt(app.getUpdatedAt());

        res.setApprovedAmount(app.getApprovedAmount());
        res.setInterestRate(app.getInterestRate());
        res.setApprovedTenureMonths(app.getApprovedTenureMonths());
        res.setManagerRemarks(app.getManagerRemarks());
        res.setReviewedBy(app.getReviewedBy());
        res.setDecisionTimestamp(app.getDecisionTimestamp());

        res.setDisbursedAt(app.getDisbursedAt());
        res.setDisbursementReference(app.getDisbursementReference());
        res.setDisbursedBy(app.getDisbursedBy());

        res.setRejectionReason(app.getRejectionReason());
        res.setRejectedBy(app.getRejectedBy());
        res.setRejectedAt(app.getRejectedAt());

        return res;
    }

    private LoanDocumentResponse mapToDocumentResponse(LoanDocument doc) {
        String downloadUrl = "/api/loans/" + doc.getLoanApplication().getApplicationReference()
                + "/documents/" + doc.getId() + "/download";

        return new LoanDocumentResponse(
                doc.getId(),
                doc.getLoanApplication().getId(),
                doc.getLoanApplication().getApplicationReference(),
                doc.getDocumentType(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getDocumentStatus(),
                doc.getRejectionReason(),
                doc.getRemarks(),
                doc.getUploadedAt(),
                doc.getReviewedAt(),
                doc.getReviewedBy(),
                downloadUrl
        );
    }
}

