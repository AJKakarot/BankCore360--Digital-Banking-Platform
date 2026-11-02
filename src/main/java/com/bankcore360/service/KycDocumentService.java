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
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KycDocumentService {

    private final KycDocumentRepository kycDocumentRepository;
    private final KycDocumentAuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final FileStorageService fileStorageService;

    public KycDocumentService(
            KycDocumentRepository kycDocumentRepository,
            KycDocumentAuditLogRepository auditLogRepository,
            CustomerRepository customerRepository,
            AppUserRepository appUserRepository,
            FileStorageService fileStorageService
    ) {
        this.kycDocumentRepository = kycDocumentRepository;
        this.auditLogRepository = auditLogRepository;
        this.customerRepository = customerRepository;
        this.appUserRepository = appUserRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public KycDocumentResponse uploadDocument(
            Long customerId,
            KycDocumentType documentType,
            MultipartFile file,
            Authentication auth
    ) {
        if (documentType == null) {
            throw new InvalidDocumentException("Document type is required. Allowed types are AADHAAR, PAN");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found"));

        validateCustomerOwnership(auth, customer);

        String secureStorageReference = fileStorageService.storeFile(file, customerId, documentType);

        KycDocument document = new KycDocument();
        document.setCustomerId(customerId);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setVerificationStatus(DocumentVerificationStatus.PENDING);
        document.setUploadedAt(LocalDateTime.now());
        document.setDocumentUrl(secureStorageReference);

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Update reference URL for endpoint retrieval
        savedDocument.setDocumentUrl("/api/customers/" + customerId + "/documents/" + savedDocument.getId() + "/file");
        savedDocument = kycDocumentRepository.save(savedDocument);

        return mapToResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getDocumentsForCustomer(Long customerId, Authentication auth) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found"));

        validateCustomerOwnership(auth, customer);

        return kycDocumentRepository.findByCustomerIdOrderByUploadedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public KycDocumentResponse getDocumentById(Long customerId, Long documentId, Authentication auth) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found"));

        validateCustomerOwnership(auth, customer);

        KycDocument document = kycDocumentRepository.findByIdAndCustomerId(documentId, customerId)
                .orElseThrow(() -> new DocumentNotFoundException("Document with ID " + documentId + " not found for customer " + customerId));

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public KycDocumentResponse getDocumentById(Long documentId, Authentication auth) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document with ID " + documentId + " not found"));

        Customer customer = customerRepository.findById(document.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + document.getCustomerId() + " not found"));

        validateCustomerOwnership(auth, customer);

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentRepository.findByVerificationStatusOrderByUploadedAtAsc(DocumentVerificationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public KycDocumentResponse reviewDocument(
            Long documentId,
            KycDocumentReviewRequest request,
            Authentication auth
    ) {
        if (request == null || request.getStatus() == null) {
            throw new InvalidDocumentException("Status is required");
        }

        if (request.getStatus() == DocumentVerificationStatus.PENDING) {
            throw new InvalidDocumentException("Target status must be VERIFIED or REJECTED");
        }

        if (request.getStatus() == DocumentVerificationStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new InvalidDocumentException("Rejection reason is mandatory when rejecting a document");
            }
        }

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document with ID " + documentId + " not found"));

        Customer customer = customerRepository.findById(document.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + document.getCustomerId() + " not found"));

        validateNotSelfReview(auth, customer);

        DocumentVerificationStatus previousStatus = document.getVerificationStatus();
        DocumentVerificationStatus newStatus = request.getStatus();

        String reviewerUsername = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        String reason = (newStatus == DocumentVerificationStatus.REJECTED)
                ? request.getRejectionReason().trim()
                : null;

        document.setVerificationStatus(newStatus);
        document.setRejectionReason(reason);
        document.setReviewedBy(reviewerUsername);
        document.setReviewedAt(LocalDateTime.now());

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Record audit decision
        KycDocumentAuditLog auditLog = new KycDocumentAuditLog(
                savedDocument.getId(),
                savedDocument.getCustomerId(),
                previousStatus,
                newStatus,
                reviewerUsername,
                reason
        );
        auditLogRepository.save(auditLog);

        return mapToResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentFile(Long customerId, Long documentId, Authentication auth) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found"));

        validateCustomerOwnership(auth, customer);

        KycDocument document = kycDocumentRepository.findByIdAndCustomerId(documentId, customerId)
                .orElseThrow(() -> new DocumentNotFoundException("Document with ID " + documentId + " not found for customer " + customerId));

        return fileStorageService.loadFileAsResource(document.getDocumentUrl());
    }

    private void validateCustomerOwnership(Authentication auth, Customer customer) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required to access KYC documents");
        }

        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHECKER")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (isStaff) {
            return;
        }

        if (!isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Access denied: You are not authorized to view or modify documents for another customer");
        }
    }

    private void validateNotSelfReview(Authentication auth, Customer customer) {
        if (auth != null && isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Reviewers are not permitted to review or verify their own KYC documents");
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

        // Check if user exists with matching email
        Optional<AppUser> userOpt = appUserRepository.findByUsername(principalName);
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            return userOpt.get().getEmail().trim().equalsIgnoreCase(c);
        }

        return false;
    }

    private KycDocumentResponse mapToResponse(KycDocument doc) {
        return new KycDocumentResponse(
                doc.getId(),
                doc.getCustomerId(),
                doc.getDocumentType(),
                doc.getDocumentUrl(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getVerificationStatus(),
                doc.getRejectionReason(),
                doc.getUploadedAt(),
                doc.getReviewedAt(),
                doc.getReviewedBy()
        );
    }
}

