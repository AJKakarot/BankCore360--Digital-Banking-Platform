package com.bankcore360.service;

import com.bankcore360.dto.LoanAnalyticsResponse;
import com.bankcore360.dto.LoanAuditLogResponse;
import com.bankcore360.entity.Customer;
import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.entity.LoanType;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoanServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanAuditService loanAuditService;

    @InjectMocks
    private AdminLoanService adminLoanService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("John Doe");
        customer.setEmail("john.doe@bankcore360.com");
    }

    @Test
    @DisplayName("Should aggregate system analytics accurately")
    void shouldAggregateAnalyticsAccurately() {
        when(customerRepository.count()).thenReturn(5L);

        LoanApplication app1 = new LoanApplication("LN-1", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app1.setStatus(LoanStatus.SUBMITTED);

        LoanApplication app2 = new LoanApplication("LN-2", customer, LoanType.HOME, new BigDecimal("500000.00"), 60, "Test");
        app2.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);

        LoanApplication app3 = new LoanApplication("LN-3", customer, LoanType.BUSINESS, new BigDecimal("200000.00"), 24, "Test");
        app3.setStatus(LoanStatus.DISBURSED);
        app3.setApprovedAmount(new BigDecimal("200000.00"));

        when(loanApplicationRepository.findAll()).thenReturn(List.of(app1, app2, app3));

        LoanAnalyticsResponse analytics = adminLoanService.getAnalytics();

        assertThat(analytics.getTotalCustomers()).isEqualTo(5L);
        assertThat(analytics.getTotalApplications()).isEqualTo(3L);
        assertThat(analytics.getPendingCheckerReviews()).isEqualTo(1L);
        assertThat(analytics.getPendingManagerApprovals()).isEqualTo(1L);
        assertThat(analytics.getDisbursedLoans()).isEqualTo(1L);
        assertThat(analytics.getTotalDisbursedLoanAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(analytics.getStatusDistribution()).containsKey("SUBMITTED");
        assertThat(analytics.getLoanTypeDistribution()).containsKey("PERSONAL");
    }

    @Test
    @DisplayName("Should return paginated system audit logs")
    void shouldReturnPaginatedAuditLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        LoanAuditLogResponse log = new LoanAuditLogResponse(1L, "LN-1", "admin", "ROLE_ADMIN", "CONFIG", null, null, "Test", LocalDateTime.now());
        Page<LoanAuditLogResponse> page = new PageImpl<>(List.of(log), pageable, 1);

        when(loanAuditService.getAllAuditLogs(pageable)).thenReturn(page);

        Page<LoanAuditLogResponse> result = adminLoanService.getSystemAuditLogs(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getApplicationReference()).isEqualTo("LN-1");
    }

    @Test
    @DisplayName("Should return all loan applications ordered by submission date descending")
    void shouldReturnAllLoanApplications() {
        LoanApplication app1 = new LoanApplication("LN-1", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        when(loanApplicationRepository.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of(app1));

        List<com.bankcore360.dto.LoanApplicationResponse> result = adminLoanService.getAllApplications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicationReference()).isEqualTo("LN-1");
    }
}

