package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.DocumentReviewRequest;
import com.bankcore360.dto.LoanApplicationResponse;
import com.bankcore360.dto.LoanDocumentResponse;
import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.LoanDocumentType;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.service.CheckerLoanService;
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

@WebMvcTest(CheckerLoanController.class)
@Import(SecurityConfig.class)
class CheckerLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckerLoanService checkerLoanService;

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("GET /api/checker/loans/pending accessible to CHECKER role")
    void shouldReturnPendingApplicationsForChecker() throws Exception {
        LoanApplicationResponse app = new LoanApplicationResponse();
        app.setId(1L);
        app.setApplicationReference("LN-2026-CHK01");
        app.setStatus(LoanStatus.SUBMITTED);

        when(checkerLoanService.getPendingApplications()).thenReturn(List.of(app));

        mockMvc.perform(get("/api/checker/loans/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationReference").value("LN-2026-CHK01"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/checker/loans/pending returns 403 Forbidden for CUSTOMER role")
    void shouldForbidCustomerFromCheckerQueue() throws Exception {
        mockMvc.perform(get("/api/checker/loans/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/checker/documents/{id}/verify verifies document")
    void shouldVerifyDocumentSuccessfully() throws Exception {
        LoanDocumentResponse doc = new LoanDocumentResponse();
        doc.setId(10L);
        doc.setDocumentType(LoanDocumentType.PAN);
        doc.setDocumentStatus(DocumentVerificationStatus.VERIFIED);

        when(checkerLoanService.verifyDocument(eq(10L), any(), any())).thenReturn(doc);

        mockMvc.perform(patch("/api/checker/documents/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DocumentReviewRequest("Verified", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentStatus").value("VERIFIED"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("POST /api/checker/loans/{id}/forward forwards to Manager")
    void shouldForwardToManagerSuccessfully() throws Exception {
        LoanApplicationResponse app = new LoanApplicationResponse();
        app.setId(1L);
        app.setApplicationReference("LN-2026-CHK01");
        app.setStatus(LoanStatus.PENDING_MANAGER_APPROVAL);

        when(checkerLoanService.forwardToManager(eq(1L), any(), any())).thenReturn(app);

        mockMvc.perform(post("/api/checker/loans/1/forward"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_APPROVAL"));
    }
}

