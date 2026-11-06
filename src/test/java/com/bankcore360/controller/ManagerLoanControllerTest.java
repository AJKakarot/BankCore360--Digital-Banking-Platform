package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.DisbursementResponse;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.ManagerDecisionRequest;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.service.ManagerLoanService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerLoanController.class)
@Import(SecurityConfig.class)
class ManagerLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManagerLoanService managerLoanService;

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("GET /api/manager/loans/pending accessible to MANAGER role")
    void shouldReturnPendingApprovalsForManager() throws Exception {
        LoanApplicationResponse app = new LoanApplicationResponse();
        app.setId(1L);
        app.setApplicationReference("LN-2026-MGR01");
        app.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);

        when(managerLoanService.getPendingApplications()).thenReturn(List.of(app));

        mockMvc.perform(get("/api/manager/loans/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationReference").value("LN-2026-MGR01"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/manager/loans/pending returns 403 Forbidden for CHECKER role")
    void shouldForbidCheckerFromManagerQueue() throws Exception {
        mockMvc.perform(get("/api/manager/loans/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("PATCH /api/manager/loans/{id}/approve approves loan")
    void shouldApproveLoanSuccessfully() throws Exception {
        ManagerDecisionRequest req = new ManagerDecisionRequest(
                new BigDecimal("150000.00"),
                new BigDecimal("10.50"),
                24,
                "Approved"
        );

        LoanApplicationResponse app = new LoanApplicationResponse();
        app.setId(1L);
        app.setApplicationReference("LN-2026-MGR01");
        app.setStatus(LoanStatus.APPROVED);
        app.setApprovedAmount(new BigDecimal("150000.00"));

        when(managerLoanService.approveApplication(eq(1L), any(), any())).thenReturn(app);

        mockMvc.perform(patch("/api/manager/loans/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(150000.00));
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("POST /api/manager/loans/{id}/disburse executes disbursement")
    void shouldDisburseLoanSuccessfully() throws Exception {
        DisbursementResponse res = new DisbursementResponse(
                "LN-2026-MGR01", "DSB-2026-ABC123",
                new BigDecimal("150000.00"), LocalDateTime.now(),
                "dev_manager", "360100000010", "DISBURSED"
        );

        when(managerLoanService.disburseLoan(eq(1L), any())).thenReturn(res);

        mockMvc.perform(post("/api/manager/loans/1/disburse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISBURSED"))
                .andExpect(jsonPath("$.disbursementReference").value("DSB-2026-ABC123"));
    }
}

