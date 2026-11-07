package com.bankcore360.service;

import com.bankcore360.dto.DisbursementResponse;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.ManagerDecisionRequest;
import com.bankcore360.entity.*;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.event.payload.EmiDuePayload;
import com.bankcore360.event.payload.LoanApprovedPayload;
import com.bankcore360.event.payload.LoanRejectedPayload;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.exception.LoanApplicationNotFoundException;
import com.bankcore360.outbox.OutboxService;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import com.bankcore360.repository.LoanRepaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ManagerLoanService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanAuditService loanAuditService;
    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;
    private final LoanEligibilityService loanEligibilityService;
    private final OutboxService outboxService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private BankVaultService bankVaultService;

    private static final String APPROVAL_EVENTS_TOPIC = "approval-events";
    private static final String REPAYMENT_EVENTS_TOPIC = "repayment-events";

    @Autowired
    public ManagerLoanService(LoanApplicationRepository loanApplicationRepository,
                              LoanRepaymentRepository loanRepaymentRepository,
                              LoanAuditService loanAuditService,
                              NotificationService notificationService,
                              AppUserRepository appUserRepository,
                              LoanEligibilityService loanEligibilityService,
                              OutboxService outboxService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.loanAuditService = loanAuditService;
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
        this.loanEligibilityService = loanEligibilityService;
        this.outboxService = outboxService;
    }

    public ManagerLoanService(LoanApplicationRepository loanApplicationRepository,
                              LoanRepaymentRepository loanRepaymentRepository,
                              LoanAuditService loanAuditService,
                              NotificationService notificationService,
                              AppUserRepository appUserRepository,
                              LoanEligibilityService loanEligibilityService) {
        this(loanApplicationRepository, loanRepaymentRepository, loanAuditService, notificationService, appUserRepository, loanEligibilityService, null);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getPendingApplications() {
        return loanApplicationRepository.findByStatusOrderBySubmittedAtAsc(LoanStatus.PENDING_MANAGER_APPROVAL)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplicationById(Long applicationId, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        return mapToResponse(app);
    }

    @Transactional
    public LoanApplicationResponse approveApplication(Long applicationId, ManagerDecisionRequest request, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        if (app.getStatus() != LoanStatus.PENDING_MANAGER_APPROVAL) {
            throw new InvalidLoanStateException("Application is in status " + app.getStatus()
                    + ". Only applications in PENDING_MANAGER_APPROVAL can be approved by Manager.");
        }

        if (request == null || request.getApprovedAmount() == null || request.getInterestRate() == null || request.getApprovedTenureMonths() == null) {
            throw new InvalidLoanStateException("Approved amount, interest rate, and approved tenure months are mandatory for approval.");
        }

        if (request.getApprovedAmount().compareTo(app.getRequestedAmount()) > 0) {
            throw new InvalidLoanStateException("Approved amount (" + request.getApprovedAmount()
                    + ") cannot exceed requested amount (" + app.getRequestedAmount() + ").");
        }

        LoanStatus prevStatus = app.getStatus();
        app.setStatus(LoanStatus.APPROVED);
        app.setApprovedAmount(request.getApprovedAmount());
        app.setInterestRate(request.getInterestRate());
        app.setApprovedTenureMonths(request.getApprovedTenureMonths());
        app.setManagerRemarks(request.getManagerRemarks() != null ? request.getManagerRemarks().trim() : "Approved by Manager");

        String managerName = auth != null ? auth.getName() : "MANAGER";
        app.setReviewedBy(managerName);
        app.setDecisionTimestamp(LocalDateTime.now());

        LoanApplication savedApp = loanApplicationRepository.save(app);

        loanAuditService.logAction(
                app.getApplicationReference(),
                managerName,
                "ROLE_MANAGER",
                "LOAN_APPROVED",
                prevStatus.name(),
                LoanStatus.APPROVED.name(),
                "Approved amount: ₹" + app.getApprovedAmount() + ", Rate: " + app.getInterestRate()
                        + "%, Tenure: " + app.getApprovedTenureMonths() + "m. Remarks: " + app.getManagerRemarks()
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Loan Approved",
                "Congratulations! Your loan application " + app.getApplicationReference()
                        + " has been APPROVED for ₹" + app.getApprovedAmount() + " at "
                        + app.getInterestRate() + "% p.a. for " + app.getApprovedTenureMonths() + " months.",
                app.getApplicationReference()
        );

        if (outboxService != null) {
            LoanApprovedPayload payload = new LoanApprovedPayload(
                    savedApp.getApplicationReference(),
                    savedApp.getCustomer().getId(),
                    savedApp.getApprovedAmount(),
                    savedApp.getInterestRate(),
                    savedApp.getApprovedTenureMonths(),
                    savedApp.getManagerRemarks(),
                    managerName,
                    savedApp.getDecisionTimestamp()
            );
            outboxService.saveEvent(APPROVAL_EVENTS_TOPIC, EventEnvelope.create("LOAN_APPROVED", savedApp.getApplicationReference(), payload));
        }

        return mapToResponse(savedApp);
    }

    @Transactional
    public LoanApplicationResponse rejectApplication(Long applicationId, ManagerDecisionRequest request, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        if (app.getStatus() == LoanStatus.DISBURSED || app.getStatus() == LoanStatus.REJECTED) {
            throw new InvalidLoanStateException("Cannot reject a loan application in status: " + app.getStatus());
        }

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().isBlank()) {
            throw new InvalidLoanStateException("Rejection reason is mandatory when rejecting a loan application.");
        }

        LoanStatus prevStatus = app.getStatus();
        app.setStatus(LoanStatus.REJECTED);
        app.setRejectionReason(request.getRejectionReason().trim());
        String managerName = auth != null ? auth.getName() : "MANAGER";
        app.setRejectedBy(managerName);
        app.setRejectedAt(LocalDateTime.now());

        LoanApplication savedApp = loanApplicationRepository.save(app);

        loanAuditService.logAction(
                app.getApplicationReference(),
                managerName,
                "ROLE_MANAGER",
                "LOAN_REJECTED",
                prevStatus.name(),
                LoanStatus.REJECTED.name(),
                "Rejected by Manager. Reason: " + app.getRejectionReason()
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Loan Application Rejected",
                "Your loan application " + app.getApplicationReference()
                        + " was rejected. Reason: " + app.getRejectionReason(),
                app.getApplicationReference()
        );

        if (outboxService != null) {
            LoanRejectedPayload payload = new LoanRejectedPayload(
                    savedApp.getApplicationReference(),
                    savedApp.getCustomer().getId(),
                    savedApp.getRejectionReason(),
                    managerName,
                    savedApp.getRejectedAt()
            );
            outboxService.saveEvent(APPROVAL_EVENTS_TOPIC, EventEnvelope.create("LOAN_REJECTED", savedApp.getApplicationReference(), payload));
        }

        return mapToResponse(savedApp);
    }

    @Transactional(rollbackFor = Exception.class)
    public DisbursementResponse disburseLoan(Long applicationId, Authentication auth) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application with ID " + applicationId + " not found"));

        validateNotSelfReview(auth, app.getCustomer());

        if (app.getStatus() != LoanStatus.APPROVED) {
            throw new InvalidLoanStateException("Application is in status " + app.getStatus()
                    + ". Only APPROVED loans can be disbursed.");
        }

        String disburseRef = generateDisbursementReference();
        String operator = auth != null ? auth.getName() : "MANAGER";

        LoanStatus prevStatus = app.getStatus();
        app.setStatus(LoanStatus.DISBURSED);
        app.setDisbursedAt(LocalDateTime.now());
        app.setDisbursementReference(disburseRef);
        app.setDisbursedBy(operator);

        LoanApplication savedApp = loanApplicationRepository.save(app);

        // Atomic & Idempotent Bank Vault Debit & Customer Account Credit
        VaultTransaction vaultTxn = null;
        if (bankVaultService != null) {
            vaultTxn = bankVaultService.disburseLoanFromVault(savedApp, operator);
        }

        String finalTxnRef = vaultTxn != null ? vaultTxn.getTransactionReference() : disburseRef;
        String creditedAccount = vaultTxn != null && vaultTxn.getBeneficiaryAccountNumber() != null
                ? vaultTxn.getBeneficiaryAccountNumber()
                : ("3601" + String.format("%08d", app.getCustomer().getId()));

        // Generate EMI Repayment Schedule automatically
        List<LoanRepayment> repayments = generateRepaymentSchedule(savedApp);

        loanAuditService.logAction(
                app.getApplicationReference(),
                operator,
                "ROLE_MANAGER",
                "LOAN_DISBURSED",
                prevStatus.name(),
                LoanStatus.DISBURSED.name(),
                "Disbursed loan amount ₹" + app.getApprovedAmount() + " from Vault to A/C " + creditedAccount + ". Txn Ref: " + finalTxnRef
        );

        notificationService.notifyUser(
                app.getCustomer().getEmail(),
                "Loan Disbursed to Account",
                "Your approved loan of ₹" + app.getApprovedAmount()
                        + " (Ref: " + app.getApplicationReference() + ") has been successfully disbursed from Bank Vault into your Account #"
                        + creditedAccount + "! Payment Txn: " + finalTxnRef + ". Monthly EMI schedule generated.",
                app.getApplicationReference()
        );

        if (outboxService != null && repayments != null && !repayments.isEmpty()) {
            LoanRepayment firstEmi = repayments.get(0);
            EmiDuePayload payload = new EmiDuePayload(
                    savedApp.getApplicationReference(),
                    firstEmi.getInstallmentNumber(),
                    firstEmi.getDueDate(),
                    firstEmi.getEmiAmount()
            );
            outboxService.saveEvent(REPAYMENT_EVENTS_TOPIC, EventEnvelope.create("EMI_DUE", savedApp.getApplicationReference(), payload));
        }

        return new DisbursementResponse(
                app.getApplicationReference(),
                finalTxnRef,
                app.getApprovedAmount(),
                app.getDisbursedAt(),
                operator,
                creditedAccount,
                "DISBURSED"
        );
    }

    private List<LoanRepayment> generateRepaymentSchedule(LoanApplication app) {
        BigDecimal principal = app.getApprovedAmount();
        BigDecimal annualRate = app.getInterestRate();
        int tenureMonths = app.getApprovedTenureMonths();

        BigDecimal monthlyEmi = loanEligibilityService.calculateEmi(principal, annualRate, tenureMonths);
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), MC);

        BigDecimal remainingBalance = principal;
        LocalDate startDate = LocalDate.now();

        List<LoanRepayment> repayments = new ArrayList<>();

        for (int i = 1; i <= tenureMonths; i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            BigDecimal interestComponent = remainingBalance.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComponent = monthlyEmi.subtract(interestComponent).setScale(2, RoundingMode.HALF_UP);

            if (i == tenureMonths || principalComponent.compareTo(remainingBalance) > 0) {
                principalComponent = remainingBalance;
                remainingBalance = BigDecimal.ZERO;
            } else {
                remainingBalance = remainingBalance.subtract(principalComponent).setScale(2, RoundingMode.HALF_UP);
            }

            LoanRepayment repayment = new LoanRepayment(
                    app,
                    i,
                    dueDate,
                    monthlyEmi,
                    principalComponent,
                    interestComponent,
                    remainingBalance
            );
            repayments.add(repayment);
        }

        return loanRepaymentRepository.saveAll(repayments);
    }

    private String generateDisbursementReference() {
        int year = Year.now().getValue();
        StringBuilder sb = new StringBuilder("DSB-").append(year).append("-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private void validateNotSelfReview(Authentication auth, Customer customer) {
        if (auth != null && isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Reviewers are not permitted to inspect or decide their own loan applications");
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
}

