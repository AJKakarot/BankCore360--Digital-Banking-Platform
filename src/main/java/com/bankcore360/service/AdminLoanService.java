package com.bankcore360.service;

import com.bankcore360.dto.LoanAnalyticsResponse;
import com.bankcore360.dto.LoanAuditLogResponse;
import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.entity.LoanType;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminLoanService {

    private final CustomerRepository customerRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanAuditService loanAuditService;

    public AdminLoanService(CustomerRepository customerRepository,
                            LoanApplicationRepository loanApplicationRepository,
                            LoanAuditService loanAuditService) {
        this.customerRepository = customerRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanAuditService = loanAuditService;
    }

    @Transactional(readOnly = true)
    public LoanAnalyticsResponse getAnalytics() {
        long totalCustomers = customerRepository.count();
        List<LoanApplication> allApps = loanApplicationRepository.findAll();
        long totalApplications = allApps.size();

        long pendingChecker = 0;
        long pendingManager = 0;
        long approved = 0;
        long rejected = 0;
        long disbursed = 0;

        Map<String, Long> statusDistribution = new HashMap<>();
        Map<String, Long> loanTypeDistribution = new HashMap<>();

        for (LoanStatus s : LoanStatus.values()) {
            statusDistribution.put(s.name(), 0L);
        }
        for (LoanType t : LoanType.values()) {
            loanTypeDistribution.put(t.name(), 0L);
        }

        BigDecimal totalApproved = BigDecimal.ZERO;
        BigDecimal totalDisbursed = BigDecimal.ZERO;

        for (LoanApplication app : allApps) {
            statusDistribution.put(app.getStatus().name(), statusDistribution.getOrDefault(app.getStatus().name(), 0L) + 1);
            loanTypeDistribution.put(app.getLoanType().name(), loanTypeDistribution.getOrDefault(app.getLoanType().name(), 0L) + 1);

            switch (app.getStatus()) {
                case SUBMITTED, UNDER_REVIEW -> pendingChecker++;
                case PENDING_MANAGER_APPROVAL -> pendingManager++;
                case APPROVED -> {
                    approved++;
                    if (app.getApprovedAmount() != null) {
                        totalApproved = totalApproved.add(app.getApprovedAmount());
                    }
                }
                case DISBURSED -> {
                    disbursed++;
                    if (app.getApprovedAmount() != null) {
                        totalApproved = totalApproved.add(app.getApprovedAmount());
                        totalDisbursed = totalDisbursed.add(app.getApprovedAmount());
                    }
                }
                case REJECTED -> rejected++;
                default -> {}
            }
        }

        return new LoanAnalyticsResponse(
                totalCustomers,
                totalApplications,
                pendingChecker,
                pendingManager,
                approved,
                rejected,
                disbursed,
                totalApproved,
                totalDisbursed,
                statusDistribution,
                loanTypeDistribution
        );
    }

    @Transactional(readOnly = true)
    public Page<LoanAuditLogResponse> getSystemAuditLogs(Pageable pageable) {
        return loanAuditService.getAllAuditLogs(pageable);
    }

    @Transactional(readOnly = true)
    public List<com.bankcore360.dto.LoanApplicationResponse> getAllApplications() {
        return loanApplicationRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private com.bankcore360.dto.LoanApplicationResponse mapToResponse(LoanApplication app) {
        com.bankcore360.dto.LoanApplicationResponse res = new com.bankcore360.dto.LoanApplicationResponse();
        res.setId(app.getId());
        res.setApplicationReference(app.getApplicationReference());
        if (app.getCustomer() != null) {
            res.setCustomerId(app.getCustomer().getId());
            res.setCustomerFullName(app.getCustomer().getFullName());
            res.setCustomerEmail(app.getCustomer().getEmail());
        }
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

