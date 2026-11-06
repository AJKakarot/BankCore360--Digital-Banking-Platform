package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.LoanAnalyticsResponse;
import com.bankcore360.dto.LoanAuditLogResponse;
import com.bankcore360.service.AdminLoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLoanController.class)
@Import(SecurityConfig.class)
class AdminLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLoanService adminLoanService;

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/loans/analytics returns 200 OK for ADMIN")
    void shouldReturnAnalyticsForAdmin() throws Exception {
        LoanAnalyticsResponse analytics = new LoanAnalyticsResponse(
                10L, 5L, 2L, 1L, 1L, 0L, 1L,
                new BigDecimal("300000.00"), new BigDecimal("150000.00"),
                Map.of("SUBMITTED", 2L), Map.of("PERSONAL", 3L)
        );

        when(adminLoanService.getAnalytics()).thenReturn(analytics);

        mockMvc.perform(get("/api/admin/loans/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(10))
                .andExpect(jsonPath("$.totalApplications").value(5));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/admin/loans/analytics returns 403 Forbidden for non-ADMIN")
    void shouldForbidNonAdminFromAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/loans/analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/loans/audit-logs returns 200 OK for ADMIN")
    void shouldReturnAuditLogsForAdmin() throws Exception {
        LoanAuditLogResponse log = new LoanAuditLogResponse(
                1L, "LN-1", "dev_customer", "ROLE_CUSTOMER",
                "LOAN_APPLICATION_SUBMITTED", null, "SUBMITTED", "Test", LocalDateTime.now()
        );

        when(adminLoanService.getSystemAuditLogs(any())).thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/loans/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].applicationReference").value("LN-1"));
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/loans returns 200 OK for ADMIN")
    void shouldReturnAllLoansForAdmin() throws Exception {
        com.bankcore360.dto.LoanApplicationResponse app = new com.bankcore360.dto.LoanApplicationResponse();
        app.setApplicationReference("LN-ALL-1");
        when(adminLoanService.getAllApplications()).thenReturn(List.of(app));

        mockMvc.perform(get("/api/admin/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationReference").value("LN-ALL-1"));
    }
}

