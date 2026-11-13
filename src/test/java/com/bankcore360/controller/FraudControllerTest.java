package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.service.FraudDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
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

@WebMvcTest(FraudController.class)
@Import(SecurityConfig.class)
public class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("POST /api/fraud/evaluate should compute score and return 200 OK")
    void testEvaluateEndpoint() throws Exception {
        EvaluateFraudRequest request = new EvaluateFraudRequest(
                "ACC-1001",
                new BigDecimal("50000.00"),
                "TRANSFER"
        );

        FraudAssessment asm = new FraudAssessment("ASM-101", "ACC-1001", null, "TRANSFER",
                new BigDecimal("50000.00"), 20, FraudRiskLevel.LOW, FraudDecision.ALLOW, "", "Nominal risk");
        asm.setId(1L);
        FraudAssessmentResponse response = new FraudAssessmentResponse(asm);

        when(fraudDetectionService.evaluateTransaction(any(EvaluateFraudRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/fraud/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentReference").value("ASM-101"))
                .andExpect(jsonPath("$.riskScore").value(20))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.decision").value("ALLOW"));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"MANAGER"})
    @DisplayName("GET /api/fraud/alerts should return paginated alerts list")
    void testGetAlertsEndpoint() throws Exception {
        FraudAssessment asm = new FraudAssessment("ASM-201", "ACC-1001", null, "WITHDRAWAL",
                new BigDecimal("800000.00"), 75, FraudRiskLevel.HIGH, FraudDecision.MANUAL_REVIEW, "RULE_LARGE_AMOUNT", "High risk");
        asm.setId(2L);

        FraudAlert alert = new FraudAlert("ALT-201", asm, "ACC-1001", null, "TXN-201",
                75, FraudRiskLevel.HIGH, FraudAlertStatus.PENDING_REVIEW, null);
        alert.setId(20L);

        PageImpl<FraudAlertResponse> page = new PageImpl<>(List.of(new FraudAlertResponse(alert)));
        when(fraudDetectionService.getAlerts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/fraud/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertReference").value("ALT-201"))
                .andExpect(jsonPath("$.content[0].riskScore").value(75))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"MANAGER"})
    @DisplayName("PATCH /api/fraud/alerts/{id}/status should update alert status and return 200 OK")
    void testUpdateAlertStatusEndpoint() throws Exception {
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest(
                FraudAlertStatus.RESOLVED_CLEARED,
                "Customer verified via OTP and call confirmation",
                "CLEAR_HOLD"
        );

        FraudAssessment asm = new FraudAssessment("ASM-201", "ACC-1001", null, "TRANSFER",
                new BigDecimal("100000.00"), 70, FraudRiskLevel.HIGH, FraudDecision.MANUAL_REVIEW, "", "");
        asm.setId(3L);

        FraudAlert alert = new FraudAlert("ALT-201", asm, "ACC-1001", null, "TXN-201",
                70, FraudRiskLevel.HIGH, FraudAlertStatus.RESOLVED_CLEARED, "manager_user");
        alert.setId(25L);

        FraudAlertResponse response = new FraudAlertResponse(alert);
        when(fraudDetectionService.updateAlertStatus(eq(25L), any(UpdateAlertStatusRequest.class), any())).thenReturn(response);

        mockMvc.perform(patch("/api/fraud/alerts/25/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_CLEARED"))
                .andExpect(jsonPath("$.assignedReviewer").value("manager_user"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("GET /api/fraud/accounts/{id}/risk-profile should return account risk profile")
    void testGetRiskProfileEndpoint() throws Exception {
        AccountRiskProfileResponse profile = new AccountRiskProfileResponse(
                "ACC-1001",
                10L,
                "Ajeet Gupta",
                FraudRiskLevel.LOW,
                25,
                12,
                0,
                false,
                LocalDateTime.now()
        );

        when(fraudDetectionService.getAccountRiskProfile("ACC-1001")).thenReturn(profile);

        mockMvc.perform(get("/api/fraud/accounts/ACC-1001/risk-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-1001"))
                .andExpect(jsonPath("$.currentRiskLevel").value("LOW"))
                .andExpect(jsonPath("$.totalEvaluations").value(12))
                .andExpect(jsonPath("$.flagged").value(false));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Unauthenticated request to /api/fraud/evaluate should return 401 Unauthorized")
    void testUnauthenticatedEvaluateFails() throws Exception {
        EvaluateFraudRequest request = new EvaluateFraudRequest("ACC-1001", new BigDecimal("100.00"), "TRANSFER");
        mockMvc.perform(post("/api/fraud/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

