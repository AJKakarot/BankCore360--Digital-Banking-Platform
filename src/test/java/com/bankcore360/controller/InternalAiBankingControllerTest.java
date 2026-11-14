package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.internal.*;
import com.bankcore360.service.InternalAiBankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalAiBankingController.class)
@Import(SecurityConfig.class)
class InternalAiBankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternalAiBankingService internalAiBankingService;

    private static final String VALID_TOKEN = "bankcore360-internal-ai-token";

    @Test
    @DisplayName("GET /api/internal/customer/accounts with valid token returns accounts")
    void shouldReturnCustomerAccountsWithValidToken() throws Exception {
        InternalCustomerAccountDto account = new InternalCustomerAccountDto(
                "360100000101", "SAVINGS", new BigDecimal("150000.00"), "INR", "ACTIVE", LocalDateTime.now()
        );
        when(internalAiBankingService.getCustomerAccounts("alice_cust")).thenReturn(List.of(account));

        mockMvc.perform(get("/api/internal/customer/accounts")
                        .header("X-Internal-Service-Token", VALID_TOKEN)
                        .header("X-Authenticated-User", "alice_cust"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("360100000101"))
                .andExpect(jsonPath("$[0].balance").value(150000.00))
                .andExpect(jsonPath("$[0].accountType").value("SAVINGS"));
    }

    @Test
    @DisplayName("GET /api/internal/customer/accounts with invalid token returns 401 Unauthorized")
    void shouldRejectInvalidServiceToken() throws Exception {
        mockMvc.perform(get("/api/internal/customer/accounts")
                        .header("X-Internal-Service-Token", "forged-or-wrong-token")
                        .header("X-Authenticated-User", "alice_cust"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/internal/customer/accounts with missing user header returns 400 Bad Request")
    void shouldRejectMissingUserHeader() throws Exception {
        mockMvc.perform(get("/api/internal/customer/accounts")
                        .header("X-Internal-Service-Token", VALID_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/internal/customer/spending-summary returns calculated totals")
    void shouldReturnCustomerSpendingSummary() throws Exception {
        InternalSpendingSummaryDto summary = new InternalSpendingSummaryDto(
                "alice_cust",
                new BigDecimal("200000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                5,
                Map.of("TRANSFERS_OUT", new BigDecimal("30000.00"), "WITHDRAWALS", new BigDecimal("20000.00")),
                "TRANSFERS_OUT",
                "Spending is conserved."
        );
        when(internalAiBankingService.getCustomerSpendingSummary("alice_cust")).thenReturn(summary);

        mockMvc.perform(get("/api/internal/customer/spending-summary")
                        .header("X-Internal-Service-Token", VALID_TOKEN)
                        .header("X-Authenticated-User", "alice_cust"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerUsername").value("alice_cust"))
                .andExpect(jsonPath("$.totalInflow").value(200000.00))
                .andExpect(jsonPath("$.totalOutflow").value(50000.00))
                .andExpect(jsonPath("$.netSavings").value(150000.00))
                .andExpect(jsonPath("$.highestSpendingCategory").value("TRANSFERS_OUT"));
    }

    @Test
    @DisplayName("GET /api/internal/customer/transactions/{ref} enforces customer ownership")
    void shouldDenyCrossCustomerTransactionAccess() throws Exception {
        when(internalAiBankingService.getTransactionDetail(eq("alice_cust"), eq("TXN-BOB-999")))
                .thenThrow(new AccessDeniedException("Access denied: You do not have ownership of transaction TXN-BOB-999"));

        mockMvc.perform(get("/api/internal/customer/transactions/TXN-BOB-999")
                        .header("X-Internal-Service-Token", VALID_TOKEN)
                        .header("X-Authenticated-User", "alice_cust"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/loans/products returns live loan catalog")
    void shouldReturnLoanProducts() throws Exception {
        InternalLoanProductDto product = new InternalLoanProductDto(
                "PERSONAL", "Personal Loan",
                new BigDecimal("25000.00"), new BigDecimal("1000000.00"),
                6, 60, new BigDecimal("11.50"), true, "Instant personal loan"
        );
        when(internalAiBankingService.getLoanProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/internal/loans/products")
                        .header("X-Internal-Service-Token", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanType").value("PERSONAL"))
                .andExpect(jsonPath("$[0].baseInterestRate").value(11.50));
    }
}

