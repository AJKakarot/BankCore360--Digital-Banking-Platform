package com.bankcore360.service;

import com.bankcore360.dto.CustomerRequest;
import com.bankcore360.dto.CustomerResponse;
import com.bankcore360.dto.KycAuditLogResponse;
import com.bankcore360.dto.KycUpdateRequest;
import com.bankcore360.entity.Customer;
import com.bankcore360.entity.KycAuditLog;
import com.bankcore360.entity.KycStatus;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.exception.DuplicateCustomerException;
import com.bankcore360.exception.InvalidKycTransitionException;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.KycAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KycAuditLogRepository kycAuditLogRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setFullName("John Doe");
        sampleCustomer.setEmail("john.doe@bankcore360.com");
        sampleCustomer.setKycStatus(KycStatus.PENDING);
    }

    @Test
    @DisplayName("Should successfully transition PENDING to UNDER_REVIEW by authorized reviewer")
    void shouldTransitionPendingToUnderReview() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        CustomerResponse response = customerService.updateKycStatus(1L, request, checkerAuth);

        assertThat(response.getKycStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(sampleCustomer.getKycStatus()).isEqualTo(KycStatus.UNDER_REVIEW);
        verify(customerRepository).save(sampleCustomer);
    }

    @Test
    @DisplayName("Should successfully transition UNDER_REVIEW to VERIFIED")
    void shouldTransitionUnderReviewToVerified() {
        sampleCustomer.setKycStatus(KycStatus.UNDER_REVIEW);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.VERIFIED);

        CustomerResponse response = customerService.updateKycStatus(1L, request, checkerAuth);

        assertThat(response.getKycStatus()).isEqualTo("VERIFIED");
        assertThat(sampleCustomer.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    @DisplayName("Should successfully transition UNDER_REVIEW to REJECTED")
    void shouldTransitionUnderReviewToRejected() {
        sampleCustomer.setKycStatus(KycStatus.UNDER_REVIEW);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.REJECTED);

        CustomerResponse response = customerService.updateKycStatus(1L, request, checkerAuth);

        assertThat(response.getKycStatus()).isEqualTo("REJECTED");
        assertThat(sampleCustomer.getKycStatus()).isEqualTo(KycStatus.REJECTED);
    }

    @Test
    @DisplayName("Should reject transition from PENDING directly to VERIFIED")
    void shouldRejectPendingDirectlyToVerified() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.VERIFIED);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(InvalidKycTransitionException.class)
                .hasMessageContaining("Cannot transition KYC status from PENDING directly to VERIFIED");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transition from PENDING directly to REJECTED")
    void shouldRejectPendingDirectlyToRejected() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.REJECTED);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(InvalidKycTransitionException.class)
                .hasMessageContaining("Cannot transition KYC status from PENDING directly to REJECTED");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject setting target status to PENDING")
    void shouldRejectSettingTargetStatusToPending() {
        sampleCustomer.setKycStatus(KycStatus.UNDER_REVIEW);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.PENDING);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(InvalidKycTransitionException.class)
                .hasMessageContaining("Target status must be UNDER_REVIEW, VERIFIED, or REJECTED");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject modifying status once VERIFIED")
    void shouldRejectModifyingOnceVerified() {
        sampleCustomer.setKycStatus(KycStatus.VERIFIED);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(InvalidKycTransitionException.class)
                .hasMessageContaining("already in terminal state VERIFIED");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject modifying status once REJECTED")
    void shouldRejectModifyingOnceRejected() {
        sampleCustomer.setKycStatus(KycStatus.REJECTED);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(InvalidKycTransitionException.class)
                .hasMessageContaining("already in terminal state REJECTED");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer ID does not exist")
    void shouldThrowWhenCustomerNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(999L, request, checkerAuth))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer with ID 999 not found");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent self-review when reviewer username equals customer email")
    void shouldPreventSelfReviewWhenEmailMatchesUsername() {
        sampleCustomer.setEmail("dev_checker@bankcore360.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        // Exact match scenario
        Authentication selfAuth = new TestingAuthenticationToken("dev_checker@bankcore360.com", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, selfAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Reviewers are not permitted to review or approve their own KYC status");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent self-review when reviewer username matches local part of customer email")
    void shouldPreventSelfReviewWhenLocalPartMatches() {
        sampleCustomer.setEmail("dev_checker@bankcore360.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        // dev_checker username reviewing dev_checker@bankcore360.com
        Authentication selfAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, selfAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Reviewers are not permitted to review or approve their own KYC status");

        verify(customerRepository, never()).save(any());
        verify(kycAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully record audit log with previous and new status and authenticated reviewer")
    void shouldRecordAuditLogOnSuccessfulStatusTransition() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        customerService.updateKycStatus(1L, request, checkerAuth);

        ArgumentCaptor<KycAuditLog> auditCaptor = ArgumentCaptor.forClass(KycAuditLog.class);
        verify(kycAuditLogRepository).save(auditCaptor.capture());

        KycAuditLog capturedAudit = auditCaptor.getValue();
        assertThat(capturedAudit.getCustomerId()).isEqualTo(1L);
        assertThat(capturedAudit.getPreviousStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(capturedAudit.getNewStatus()).isEqualTo(KycStatus.UNDER_REVIEW);
        assertThat(capturedAudit.getReviewerUsername()).isEqualTo("dev_checker");
        assertThat(capturedAudit.getCreatedAt()).isNotNull();
        assertThat(capturedAudit.getReviewedBy()).isEqualTo("dev_checker");
        assertThat(capturedAudit.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should record audit log using SecurityContextHolder when authentication parameter is null")
    void shouldRecordAuditLogFromSecurityContextWhenAuthParamIsNull() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication secContextAuth = new TestingAuthenticationToken("sec_checker", null, "ROLE_CHECKER");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(secContextAuth);
        try {
            KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);
            customerService.updateKycStatus(1L, request, null);

            ArgumentCaptor<KycAuditLog> auditCaptor = ArgumentCaptor.forClass(KycAuditLog.class);
            verify(kycAuditLogRepository).save(auditCaptor.capture());

            KycAuditLog capturedAudit = auditCaptor.getValue();
            assertThat(capturedAudit.getReviewerUsername()).isEqualTo("sec_checker");
            assertThat(capturedAudit.getPreviousStatus()).isEqualTo(KycStatus.PENDING);
            assertThat(capturedAudit.getNewStatus()).isEqualTo(KycStatus.UNDER_REVIEW);
            assertThat(capturedAudit.getCreatedAt()).isNotNull();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Should record multiple audit records across sequential status transitions")
    void shouldRecordMultipleAuditLogsAcrossSequentialTransitions() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First transition: PENDING -> UNDER_REVIEW by dev_checker
        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        customerService.updateKycStatus(1L, new KycUpdateRequest(KycStatus.UNDER_REVIEW), checkerAuth);

        // Second transition: UNDER_REVIEW -> VERIFIED by dev_manager
        Authentication managerAuth = new TestingAuthenticationToken("dev_manager", null, "ROLE_MANAGER");
        customerService.updateKycStatus(1L, new KycUpdateRequest(KycStatus.VERIFIED), managerAuth);

        ArgumentCaptor<KycAuditLog> auditCaptor = ArgumentCaptor.forClass(KycAuditLog.class);
        verify(kycAuditLogRepository, times(2)).save(auditCaptor.capture());

        List<KycAuditLog> capturedLogs = auditCaptor.getAllValues();
        assertThat(capturedLogs).hasSize(2);

        KycAuditLog log1 = capturedLogs.get(0);
        assertThat(log1.getPreviousStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(log1.getNewStatus()).isEqualTo(KycStatus.UNDER_REVIEW);
        assertThat(log1.getReviewerUsername()).isEqualTo("dev_checker");
        assertThat(log1.getCreatedAt()).isNotNull();

        KycAuditLog log2 = capturedLogs.get(1);
        assertThat(log2.getPreviousStatus()).isEqualTo(KycStatus.UNDER_REVIEW);
        assertThat(log2.getNewStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(log2.getReviewerUsername()).isEqualTo("dev_manager");
        assertThat(log2.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when audit log save fails and ensure updateKycStatus is transactional")
    void shouldThrowAndFailAtomicallyWhenAuditPersistenceFails() throws NoSuchMethodException {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycAuditLogRepository.save(any(KycAuditLog.class)))
                .thenThrow(new DataIntegrityViolationException("Database write failure for audit log"));

        Authentication checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
        KycUpdateRequest request = new KycUpdateRequest(KycStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> customerService.updateKycStatus(1L, request, checkerAuth))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Database write failure for audit log");

        // Verify updateKycStatus is declared @Transactional to guarantee rollback on unchecked exception
        Transactional annotation = CustomerService.class
                .getMethod("updateKycStatus", Long.class, KycUpdateRequest.class, Authentication.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("Should return audit records in chronological order for existing customer")
    void shouldReturnAuditRecordsInChronologicalOrder() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        KycAuditLog log1 = new KycAuditLog(1L, KycStatus.PENDING, KycStatus.UNDER_REVIEW, "dev_checker", now.minusMinutes(10));
        KycAuditLog log2 = new KycAuditLog(1L, KycStatus.UNDER_REVIEW, KycStatus.VERIFIED, "dev_manager", now);

        when(kycAuditLogRepository.findByCustomerIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(log1, log2));

        List<KycAuditLogResponse> history = customerService.getKycAuditHistory(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getCustomerId()).isEqualTo(1L);
        assertThat(history.get(0).getPreviousStatus()).isEqualTo("PENDING");
        assertThat(history.get(0).getNewStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(history.get(0).getReviewerUsername()).isEqualTo("dev_checker");
        assertThat(history.get(0).getCreatedAt()).isEqualTo(log1.getCreatedAt());

        assertThat(history.get(1).getCustomerId()).isEqualTo(1L);
        assertThat(history.get(1).getPreviousStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(history.get(1).getNewStatus()).isEqualTo("VERIFIED");
        assertThat(history.get(1).getReviewerUsername()).isEqualTo("dev_manager");
        assertThat(history.get(1).getCreatedAt()).isEqualTo(log2.getCreatedAt());
    }

    @Test
    @DisplayName("Should return empty list when existing customer has no audit logs")
    void shouldReturnEmptyListWhenNoAuditHistory() {
        when(customerRepository.existsById(1L)).thenReturn(true);
        when(kycAuditLogRepository.findByCustomerIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of());

        List<KycAuditLogResponse> history = customerService.getKycAuditHistory(1L);

        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when fetching audit history for non-existent customer")
    void shouldThrowCustomerNotFoundWhenFetchingAuditHistory() {
        when(customerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.getKycAuditHistory(999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer with ID 999 not found");

        verify(kycAuditLogRepository, never()).findByCustomerIdOrderByCreatedAtAscIdAsc(any());
    }
}
