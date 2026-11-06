package com.bankcore360.service;

import com.bankcore360.dto.LoanApplicationRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.dto.LoanRepaymentResponse;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InvalidLoanStateException;
import com.bankcore360.exception.LoanApplicationNotFoundException;
import com.bankcore360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerLoanServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanDocumentRepository loanDocumentRepository;
    @Mock
    private LoanRepaymentRepository loanRepaymentRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private LoanApplicationReferenceGenerator referenceGenerator;
    @Mock
    private LoanDocumentStorageService documentStorageService;
    @Mock
    private LoanAuditService loanAuditService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CustomerLoanService customerLoanService;

    private Customer customer;
    private AppUser appUser;
    private Authentication customerAuth;
    private Authentication anotherCustomerAuth;
    private Authentication checkerAuth;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(10L);
        customer.setFullName("John Doe");
        customer.setEmail("john.doe@bankcore360.com");
        customer.setKycStatus(KycStatus.VERIFIED);

        appUser = new AppUser("john.doe", "john.doe@bankcore360.com", "password", UserRole.CUSTOMER);

        customerAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_CUSTOMER");
        anotherCustomerAuth = new TestingAuthenticationToken("jane.smith", null, "ROLE_CUSTOMER");
        checkerAuth = new TestingAuthenticationToken("dev_checker", null, "ROLE_CHECKER");
    }

    @Test
    @DisplayName("Should submit loan application and create SUBMITTED record with audit and notification")
    void shouldSubmitLoanApplicationSuccessfully() {
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByEmail("john.doe@bankcore360.com")).thenReturn(Optional.of(customer));
        when(referenceGenerator.generateUniqueReference()).thenReturn("LN-2026-AB12CD");

        when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> {
            LoanApplication app = inv.getArgument(0);
            app.setId(100L);
            return app;
        });

        LoanApplicationRequest request = new LoanApplicationRequest(
                LoanType.PERSONAL,
                new BigDecimal("150000.00"),
                24,
                "Higher education course"
        );

        LoanApplicationResponse response = customerLoanService.applyLoan(request, customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationReference()).isEqualTo("LN-2026-AB12CD");
        assertThat(response.getStatus()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(response.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));

        verify(loanAuditService).logAction(
                eq("LN-2026-AB12CD"),
                eq("john.doe"),
                eq("ROLE_CUSTOMER"),
                eq("LOAN_APPLICATION_SUBMITTED"),
                any(),
                eq("SUBMITTED"),
                any()
        );

        verify(notificationService).notifyUser(eq("john.doe"), contains("Submitted"), any(), eq("LN-2026-AB12CD"));
    }

    @Test
    @DisplayName("Should allow customer to view own application details")
    void shouldAllowCustomerToViewOwnApplication() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app.setId(101L);
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        LoanApplicationResponse response = customerLoanService.getApplicationByReference("LN-2026-TEST01", customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationReference()).isEqualTo("LN-2026-TEST01");
    }

    @Test
    @DisplayName("Should forbid customer from accessing another customer's loan application")
    void shouldForbidCustomerFromAccessingAnotherCustomerApplication() {
        Customer otherCustomer = new Customer();
        otherCustomer.setId(20L);
        otherCustomer.setEmail("other@bankcore360.com");
        otherCustomer.setFullName("Other Person");

        LoanApplication app = new LoanApplication("LN-2026-OTHER", otherCustomer, LoanType.HOME, new BigDecimal("500000.00"), 60, "Home");
        when(loanApplicationRepository.findByApplicationReference("LN-2026-OTHER")).thenReturn(Optional.of(app));
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(appUser));

        assertThatThrownBy(() -> customerLoanService.getApplicationByReference("LN-2026-OTHER", customerAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to access another customer's loan application");
    }

    @Test
    @DisplayName("Should allow staff (CHECKER) to view any customer application")
    void shouldAllowStaffToViewAnyApplication() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        LoanApplicationResponse response = customerLoanService.getApplicationByReference("LN-2026-TEST01", checkerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationReference()).isEqualTo("LN-2026-TEST01");
    }

    @Test
    @DisplayName("Should successfully upload document for loan application and mark as PENDING")
    void shouldUploadDocumentSuccessfully() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app.setId(101L);
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "dummy content".getBytes());
        when(documentStorageService.storeFile(file, "LN-2026-TEST01", LoanDocumentType.PAN)).thenReturn("secure_pan.pdf");

        when(loanDocumentRepository.save(any(LoanDocument.class))).thenAnswer(inv -> {
            LoanDocument d = inv.getArgument(0);
            d.setId(501L);
            return d;
        });

        LoanDocumentResponse response = customerLoanService.uploadDocument("LN-2026-TEST01", LoanDocumentType.PAN, file, customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentType()).isEqualTo(LoanDocumentType.PAN);
        assertThat(response.getDocumentStatus()).isEqualTo(DocumentVerificationStatus.PENDING);
        verify(loanAuditService).logAction(eq("LN-2026-TEST01"), eq("john.doe"), any(), eq("DOCUMENT_UPLOADED"), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject document upload if application is REJECTED or DISBURSED")
    void shouldRejectDocumentUploadIfAppRejected() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app.setStatus(LoanStatus.REJECTED);
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "dummy content".getBytes());

        assertThatThrownBy(() -> customerLoanService.uploadDocument("LN-2026-TEST01", LoanDocumentType.PAN, file, customerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("Cannot upload documents to a loan application in status: REJECTED");
    }

    @Test
    @DisplayName("Should successfully record simulated EMI repayment")
    void shouldPayEmiSuccessfully() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app.setId(101L);
        app.setStatus(LoanStatus.DISBURSED);
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        LoanRepayment installment = new LoanRepayment(app, 1, LocalDate.now().plusMonths(1), new BigDecimal("8884.88"), new BigDecimal("7884.88"), new BigDecimal("1000.00"), new BigDecimal("92115.12"));
        installment.setId(901L);
        when(loanRepaymentRepository.findById(901L)).thenReturn(Optional.of(installment));
        when(loanRepaymentRepository.save(any(LoanRepayment.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanRepaymentResponse response = customerLoanService.payEmiInstallment("LN-2026-TEST01", 901L, customerAuth);

        assertThat(response.getStatus()).isEqualTo(RepaymentStatus.PAID);
        assertThat(response.getPaymentReference()).startsWith("PAY-");
        assertThat(response.getPaidAt()).isNotNull();
        verify(loanAuditService).logAction(eq("LN-2026-TEST01"), eq("john.doe"), any(), eq("EMI_INSTALLMENT_PAID"), any(), eq("PAID"), any());
    }

    @Test
    @DisplayName("Should reject paying already PAID installment")
    void shouldRejectPayingAlreadyPaidInstallment() {
        LoanApplication app = new LoanApplication("LN-2026-TEST01", customer, LoanType.PERSONAL, new BigDecimal("100000.00"), 12, "Test");
        app.setId(101L);
        app.setStatus(LoanStatus.DISBURSED);
        when(loanApplicationRepository.findByApplicationReference("LN-2026-TEST01")).thenReturn(Optional.of(app));

        LoanRepayment installment = new LoanRepayment(app, 1, LocalDate.now(), new BigDecimal("8884.88"), new BigDecimal("7884.88"), new BigDecimal("1000.00"), BigDecimal.ZERO);
        installment.setId(901L);
        installment.setStatus(RepaymentStatus.PAID);
        when(loanRepaymentRepository.findById(901L)).thenReturn(Optional.of(installment));

        assertThatThrownBy(() -> customerLoanService.payEmiInstallment("LN-2026-TEST01", 901L, customerAuth))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessageContaining("Installment #1 is already paid");
    }
}

