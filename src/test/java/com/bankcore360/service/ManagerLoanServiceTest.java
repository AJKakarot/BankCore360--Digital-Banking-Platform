package com.bankcore360.service;

import com.bankcore360.dto.DisbursementResponse;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.ManagerDecisionRequest;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import com.bankcore360.repository.LoanRepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ManagerLoanServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanRepaymentRepository loanRepaymentRepository;
    @Mock
    private LoanAuditService loanAuditService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private LoanEligibilityService loanEligibilityService;

    @InjectMocks
    private ManagerLoanService managerLoanService;

    private Customer customer;
    private LoanApplication application;
    private Authentication managerAuth;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("John Doe");
        customer.setEmail("john.doe@bankcore360.com");

        application = new LoanApplication("LN-2026-MGR01", customer, LoanType.PERSONAL, new BigDecimal("200000.00"), 24, "Course fees");
        application.setId(20L);
        application.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);

        managerAuth = new TestingAuthenticationToken("dev_manager", null, "ROLE_MANAGER");
    }

    @Test
    @DisplayName("Should approve loan application, log audit, and notify applicant")
    void shouldApproveApplicationSuccessfully() {
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ManagerDecisionRequest req = new ManagerDecisionRequest(
                new BigDecimal("180000.00"),
                new BigDecimal("10.50"),
                24,
                "Approved with slight amount haircut"
        );

        LoanApplicationResponse response = managerLoanService.approveApplication(20L, req, managerAuth);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(response.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("180000.00"));
        assertThat(response.getInterestRate()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(response.getReviewedBy()).isEqualTo("dev_manager");

        verify(loanAuditService).logAction(
                eq("LN-2026-MGR01"),
                eq("dev_manager"),
                eq("ROLE_MANAGER"),
                eq("LOAN_APPROVED"),
                eq("PENDING_MANAGER_APPROVAL"),
                eq("APPROVED"),
                any()
        );
        verify(notificationService).notifyUser(eq("john.doe@bankcore360.com"), contains("Approved"), any(), eq("LN-2026-MGR01"));
    }

    @Test
    @DisplayName("Should reject approval if approved amount exceeds requested amount")
    void shouldRejectWhenApprovedAmountExceedsRequested() {
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        ManagerDecisionRequest req = new ManagerDecisionRequest(
                new BigDecimal("250000.00"), // Requested was 200,000!
                new BigDecimal("10.50"),
                24,
                "Over limit"
        );

        assertThatThrownBy(() -> managerLoanService.approveApplication(20L, req, managerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("cannot exceed requested amount");
    }

    @Test
    @DisplayName("Should reject approval if application is not in PENDING_MANAGER_APPROVAL status")
    void shouldRejectApprovalIfWrongStatus() {
        application.setStatus(LoanStatus.SUBMITTED); // Not yet document-verified
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        ManagerDecisionRequest req = new ManagerDecisionRequest(new BigDecimal("100000.00"), new BigDecimal("10.50"), 12, "Approve");

        assertThatThrownBy(() -> managerLoanService.approveApplication(20L, req, managerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("Only applications in PENDING_MANAGER_APPROVAL can be approved");
    }

    @Test
    @DisplayName("Should reject loan application when mandatory reason provided")
    void shouldRejectApplicationSuccessfully() {
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ManagerDecisionRequest req = new ManagerDecisionRequest();
        req.setRejectionReason("Applicant credit profile does not meet minimum risk appetite");

        LoanApplicationResponse response = managerLoanService.rejectApplication(20L, req, managerAuth);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(response.getRejectionReason()).contains("minimum risk appetite");
        verify(loanAuditService).logAction(eq("LN-2026-MGR01"), eq("dev_manager"), any(), eq("LOAN_REJECTED"), any(), eq("REJECTED"), any());
    }

    @Test
    @DisplayName("Should disburse APPROVED loan and generate full reducing-balance EMI schedule")
    void shouldDisburseLoanSuccessfully() {
        application.setStatus(LoanStatus.APPROVED);
        application.setApprovedAmount(new BigDecimal("120000.00"));
        application.setInterestRate(new BigDecimal("12.00"));
        application.setApprovedTenureMonths(12);

        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loanEligibilityService.calculateEmi(any(), any(), eq(12))).thenReturn(new BigDecimal("10661.85"));

        DisbursementResponse response = managerLoanService.disburseLoan(20L, managerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("DISBURSED");
        assertThat(response.getDisbursementReference()).startsWith("DSB-");
        assertThat(response.getDisbursedAmount()).isEqualByComparingTo(new BigDecimal("120000.00"));

        // Verify EMI repayment schedule generation: exactly 12 monthly installments
        ArgumentCaptor<List<LoanRepayment>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanRepaymentRepository).saveAll(captor.capture());
        List<LoanRepayment> generatedRepayments = captor.getValue();
        assertThat(generatedRepayments).hasSize(12);
        assertThat(generatedRepayments.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(generatedRepayments.get(11).getInstallmentNumber()).isEqualTo(12);
        assertThat(generatedRepayments.get(11).getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(loanAuditService).logAction(eq("LN-2026-MGR01"), eq("dev_manager"), any(), eq("LOAN_DISBURSED"), eq("APPROVED"), eq("DISBURSED"), any());
    }

    @Test
    @DisplayName("Should reject disbursement if application is not in APPROVED status")
    void shouldRejectDisbursementIfNotApproved() {
        application.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> managerLoanService.disburseLoan(20L, managerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("Only APPROVED loans can be disbursed");
    }

    @Test
    @DisplayName("Should prevent Manager self-review if application belongs to same user")
    void shouldPreventManagerSelfReview() {
        Authentication selfAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_MANAGER");
        when(loanApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        ManagerDecisionRequest req = new ManagerDecisionRequest(new BigDecimal("100000.00"), new BigDecimal("10.50"), 12, "Self");

        assertThatThrownBy(() -> managerLoanService.approveApplication(20L, req, selfAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Reviewers are not permitted to inspect or decide their own loan applications");
    }
}

