package com.bankcore360.service;

import com.bankcore360.dto.LoanApplicationRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.dto.LoanRepaymentResponse;
import com.bankcore360.entity.*;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.event.payload.EmiPaidPayload;
import com.bankcore360.event.payload.LoanApplicationSubmittedPayload;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.exception.LoanApplicationNotFoundException;
import com.bankcore360.exception.LoanDocumentNotFoundException;
import com.bankcore360.outbox.OutboxService;
import com.bankcore360.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerLoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final LoanApplicationReferenceGenerator referenceGenerator;
    private final LoanDocumentStorageService documentStorageService;
    private final LoanAuditService loanAuditService;
    private final NotificationService notificationService;
    private final OutboxService outboxService;

    private static final String LOAN_EVENTS_TOPIC = "loan-events";
    private static final String REPAYMENT_EVENTS_TOPIC = "repayment-events";

    @Autowired
    public CustomerLoanService(LoanApplicationRepository loanApplicationRepository,
                               LoanDocumentRepository loanDocumentRepository,
                               LoanRepaymentRepository loanRepaymentRepository,
                               CustomerRepository customerRepository,
                               AppUserRepository appUserRepository,
                               LoanApplicationReferenceGenerator referenceGenerator,
                               LoanDocumentStorageService documentStorageService,
                               LoanAuditService loanAuditService,
                               NotificationService notificationService,
                               OutboxService outboxService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanDocumentRepository = loanDocumentRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.customerRepository = customerRepository;
        this.appUserRepository = appUserRepository;
        this.referenceGenerator = referenceGenerator;
        this.documentStorageService = documentStorageService;
        this.loanAuditService = loanAuditService;
        this.notificationService = notificationService;
        this.outboxService = outboxService;
    }

    public CustomerLoanService(LoanApplicationRepository loanApplicationRepository,
                               LoanDocumentRepository loanDocumentRepository,
                               LoanRepaymentRepository loanRepaymentRepository,
                               CustomerRepository customerRepository,
                               AppUserRepository appUserRepository,
                               LoanApplicationReferenceGenerator referenceGenerator,
                               LoanDocumentStorageService documentStorageService,
                               LoanAuditService loanAuditService,
                               NotificationService notificationService) {
        this(loanApplicationRepository, loanDocumentRepository, loanRepaymentRepository,
                customerRepository, appUserRepository, referenceGenerator,
                documentStorageService, loanAuditService, notificationService, null);
    }

    @Transactional
    public LoanApplicationResponse applyLoan(LoanApplicationRequest request, Authentication auth) {
        Customer customer = resolveCustomerFromAuth(auth);

        String reference = referenceGenerator.generateUniqueReference();

        LoanApplication loanApp = new LoanApplication(
                reference,
                customer,
                request.getLoanType(),
                request.getRequestedAmount(),
                request.getTenureMonths(),
                request.getPurpose()
        );

        LoanApplication savedApp = loanApplicationRepository.save(loanApp);

        String principalName = auth != null ? auth.getName() : "CUSTOMER";
        loanAuditService.logAction(
                reference,
                principalName,
                "ROLE_CUSTOMER",
                "LOAN_APPLICATION_SUBMITTED",
                null,
                LoanStatus.SUBMITTED.name(),
                "Customer submitted loan application for amount ₹" + request.getRequestedAmount()
        );

        notificationService.notifyUser(
                principalName,
                "Loan Application Submitted",
                "Your loan application " + reference + " of ₹" + request.getRequestedAmount()
                        + " (" + request.getLoanType() + ") has been submitted successfully.",
                reference
        );

        if (outboxService != null) {
            LoanApplicationSubmittedPayload payload = new LoanApplicationSubmittedPayload(
                    savedApp.getApplicationReference(),
                    customer.getId(),
                    savedApp.getLoanType(),
                    savedApp.getRequestedAmount(),
                    savedApp.getSubmittedAt()
            );
            outboxService.saveEvent(LOAN_EVENTS_TOPIC, EventEnvelope.create("LOAN_APPLICATION_SUBMITTED", savedApp.getApplicationReference(), payload));
        }

        return mapToResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getMyApplications(Authentication auth) {
        Customer customer = resolveCustomerFromAuth(auth);
        return loanApplicationRepository.findByCustomerOrderBySubmittedAtDesc(customer)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplicationByReference(String reference, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationAccess(auth, loanApp);

        return mapToResponse(loanApp);
    }

    @Transactional
    public LoanDocumentResponse uploadDocument(String reference, LoanDocumentType documentType,
                                               MultipartFile file, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationOwnership(auth, loanApp);

        if (loanApp.getStatus() == LoanStatus.REJECTED || loanApp.getStatus() == LoanStatus.DISBURSED) {
            throw new InvalidLoanStateException("Cannot upload documents to a loan application in status: " + loanApp.getStatus());
        }

        String secureFilename = documentStorageService.storeFile(file, reference, documentType);

        LoanDocument document = new LoanDocument(
                loanApp,
                documentType,
                secureFilename,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );

        LoanDocument savedDoc = loanDocumentRepository.save(document);

        String principalName = auth != null ? auth.getName() : "CUSTOMER";
        loanAuditService.logAction(
                reference,
                principalName,
                "ROLE_CUSTOMER",
                "DOCUMENT_UPLOADED",
                null,
                savedDoc.getDocumentStatus().name(),
                "Uploaded document: " + documentType + " (" + savedDoc.getFileName() + ")"
        );

        notificationService.notifyUser(
                principalName,
                "Document Uploaded",
                "Your document " + documentType + " was uploaded for application " + reference + ".",
                reference
        );

        return mapToDocumentResponse(savedDoc);
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponse> getDocumentsForApplication(String reference, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationAccess(auth, loanApp);

        return loanDocumentRepository.findByLoanApplicationOrderByUploadedAtDesc(loanApp)
                .stream()
                .map(this::mapToDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(String reference, Long documentId, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationAccess(auth, loanApp);

        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .filter(d -> d.getLoanApplication().getId().equals(loanApp.getId()))
                .orElseThrow(() -> new LoanDocumentNotFoundException("Document with ID " + documentId + " not found in application " + reference));

        return documentStorageService.loadFileAsResource(doc.getStorageReference());
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponse> getRepaymentSchedule(String reference, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationAccess(auth, loanApp);

        return loanRepaymentRepository.findByLoanApplicationApplicationReferenceOrderByInstallmentNumberAsc(reference)
                .stream()
                .map(this::mapToRepaymentResponse)
                .toList();
    }

    @Transactional
    public LoanRepaymentResponse payEmiInstallment(String reference, Long repaymentId, Authentication auth) {
        LoanApplication loanApp = loanApplicationRepository.findByApplicationReference(reference)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with reference " + reference + " not found"));

        validateApplicationOwnership(auth, loanApp);

        if (loanApp.getStatus() != LoanStatus.DISBURSED) {
            throw new InvalidLoanStateException("Repayments can only be made on DISBURSED loan applications.");
        }

        LoanRepayment installment = loanRepaymentRepository.findById(repaymentId)
                .filter(r -> r.getLoanApplication().getId().equals(loanApp.getId()))
                .orElseThrow(() -> new LoanApplicationNotFoundException("Installment ID " + repaymentId + " not found"));

        if (installment.getStatus() == RepaymentStatus.PAID) {
            throw new InvalidLoanStateException("Installment #" + installment.getInstallmentNumber() + " is already paid.");
        }

        installment.setStatus(RepaymentStatus.PAID);
        installment.setPaidAt(LocalDateTime.now());
        installment.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        LoanRepayment savedRepayment = loanRepaymentRepository.save(installment);

        String principalName = auth != null ? auth.getName() : "CUSTOMER";
        loanAuditService.logAction(
                reference,
                principalName,
                "ROLE_CUSTOMER",
                "EMI_INSTALLMENT_PAID",
                RepaymentStatus.UPCOMING.name(),
                RepaymentStatus.PAID.name(),
                "Paid installment #" + savedRepayment.getInstallmentNumber() + " amount ₹" + savedRepayment.getEmiAmount()
        );

        notificationService.notifyUser(
                principalName,
                "EMI_PAID",
                "EMI Payment Received",
                "Payment of ₹" + savedRepayment.getEmiAmount() + " for installment #"
                        + savedRepayment.getInstallmentNumber() + " recorded successfully. Ref: "
                        + savedRepayment.getPaymentReference(),
                reference
        );

        if (outboxService != null) {
            EmiPaidPayload payload = new EmiPaidPayload(
                    reference,
                    savedRepayment.getInstallmentNumber(),
                    savedRepayment.getEmiAmount(),
                    savedRepayment.getPaymentReference(),
                    savedRepayment.getPaidAt()
            );
            outboxService.saveEvent(REPAYMENT_EVENTS_TOPIC, EventEnvelope.create("EMI_PAID", reference, payload));
        }

        return mapToRepaymentResponse(savedRepayment);
    }

    public Customer resolveCustomerFromAuth(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required to access loan services");
        }
        String principal = auth.getName().trim();

        // 1. Direct match by email
        if (principal.contains("@")) {
            Optional<Customer> byEmail = customerRepository.findByEmail(principal.toLowerCase());
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }

        // 2. Lookup AppUser by username OR email
        Optional<AppUser> userOpt = appUserRepository.findByUsername(principal)
                .or(() -> appUserRepository.findByEmail(principal.toLowerCase()));

        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            Optional<Customer> byUserEmail = customerRepository.findByEmail(userOpt.get().getEmail().toLowerCase());
            if (byUserEmail.isPresent()) {
                return byUserEmail.get();
            }
        }

        // 3. Match against any Customer by name or email
        List<Customer> allCustomers = customerRepository.findAll();
        for (Customer c : allCustomers) {
            if (isSamePerson(principal, c.getEmail()) || principal.equalsIgnoreCase(c.getFullName())) {
                return c;
            }
        }

        // 4. Auto-provision Customer profile for authenticated user
        if (userOpt.isPresent()) {
            Customer autoCustomer = new Customer();
            autoCustomer.setFullName(userOpt.get().getUsername());
            String email = userOpt.get().getEmail() != null ? userOpt.get().getEmail().toLowerCase() : principal.toLowerCase() + "@bankcore360.com";
            autoCustomer.setEmail(email);
            autoCustomer.setKycStatus(KycStatus.PENDING);
            return customerRepository.save(autoCustomer);
        }

        // 5. Fallback auto-provision for valid authenticated principal
        Customer autoCustomer = new Customer();
        autoCustomer.setFullName(principal.contains("@") ? principal.substring(0, principal.indexOf('@')) : principal);
        autoCustomer.setEmail(principal.contains("@") ? principal.toLowerCase() : principal.toLowerCase() + "@bankcore360.com");
        autoCustomer.setKycStatus(KycStatus.PENDING);
        return customerRepository.save(autoCustomer);
    }

    private void validateApplicationAccess(Authentication auth, LoanApplication application) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required to view loan application");
        }

        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHECKER")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (isStaff) {
            return;
        }

        validateApplicationOwnership(auth, application);
    }

    private void validateApplicationOwnership(Authentication auth, LoanApplication application) {
        Customer customer = application.getCustomer();
        if (!isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Access denied: You are not authorized to access another customer's loan application");
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

    private LoanRepaymentResponse mapToRepaymentResponse(LoanRepayment rep) {
        return new LoanRepaymentResponse(
                rep.getId(),
                rep.getInstallmentNumber(),
                rep.getDueDate(),
                rep.getEmiAmount(),
                rep.getPrincipalComponent(),
                rep.getInterestComponent(),
                rep.getOutstandingBalance(),
                rep.getStatus(),
                rep.getPaidAt(),
                rep.getPaymentReference()
        );
    }
}

