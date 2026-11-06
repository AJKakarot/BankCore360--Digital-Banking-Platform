package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.*;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.entity.LoanType;
import com.bankcore360.entity.RepaymentStatus;
import com.bankcore360.service.CustomerLoanService;
import com.bankcore360.service.LoanEligibilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanApplicationController.class)
@Import(SecurityConfig.class)
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerLoanService customerLoanService;

    @MockitoBean
    private LoanEligibilityService loanEligibilityService;

    @Test
    @DisplayName("POST /api/loans/calculator is publicly accessible and returns 200")
    void shouldAllowPublicAccessToCalculator() throws Exception {
        LoanEligibilityRequest request = new LoanEligibilityRequest(
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                12,
                new BigDecimal("10.50")
        );

        LoanEligibilityResponse response = new LoanEligibilityResponse(
                new BigDecimal("8814.86"),
                new BigDecimal("5778.32"),
                new BigDecimal("105778.32"),
                new BigDecimal("17.63"),
                true,
                new BigDecimal("283612.45"),
                "Indicative Eligible"
        );

        when(loanEligibilityService.calculateEligibility(any())).thenReturn(response);

        mockMvc.perform(post("/api/loans/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.estimatedMonthlyEmi").value(8814.86));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/loans/apply with CUSTOMER role returns 201 Created")
    void shouldCreateLoanApplicationSuccessfully() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                LoanType.PERSONAL,
                new BigDecimal("100000.00"),
                12,
                "Higher education course"
        );

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(1L);
        response.setApplicationReference("LN-2026-TEST01");
        response.setLoanType(LoanType.PERSONAL);
        response.setRequestedAmount(new BigDecimal("100000.00"));
        response.setTenureMonths(12);
        response.setStatus(LoanStatus.SUBMITTED);
        response.setSubmittedAt(LocalDateTime.now());

        when(customerLoanService.applyLoan(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/loans/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationReference").value("LN-2026-TEST01"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /api/loans/apply without authentication returns 401 Unauthorized")
    void shouldRejectAnonymousLoanApply() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                LoanType.PERSONAL,
                new BigDecimal("100000.00"),
                12,
                "Higher education course"
        );

        mockMvc.perform(post("/api/loans/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/loans/my-applications returns 200 for CUSTOMER")
    void shouldReturnMyApplications() throws Exception {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(1L);
        response.setApplicationReference("LN-2026-TEST01");
        response.setStatus(LoanStatus.SUBMITTED);

        when(customerLoanService.getMyApplications(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/loans/my-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationReference").value("LN-2026-TEST01"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/loans/my-applications returns 403 Forbidden for non-CUSTOMER")
    void shouldForbidCheckerFromAccessingMyApplications() throws Exception {
        mockMvc.perform(get("/api/loans/my-applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/loans/{ref}/repayments/{id}/pay returns 200 OK")
    void shouldPayEmiInstallmentSuccessfully() throws Exception {
        LoanRepaymentResponse response = new LoanRepaymentResponse(
                1L, 1, LocalDate.now(), new BigDecimal("8814.86"),
                new BigDecimal("7814.86"), new BigDecimal("1000.00"),
                BigDecimal.ZERO, RepaymentStatus.PAID, LocalDateTime.now(), "PAY-ABC12345"
        );

        when(customerLoanService.payEmiInstallment(eq("LN-2026-TEST01"), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/loans/LN-2026-TEST01/repayments/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentReference").value("PAY-ABC12345"));
    }
}

