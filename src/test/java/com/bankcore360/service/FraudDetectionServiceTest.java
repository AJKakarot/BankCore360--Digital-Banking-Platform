package com.bankcore360.service;

import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.repository.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private FraudInvestigationRepository fraudInvestigationRepository;

    @Mock
    private RedisVelocityService redisVelocityService;

    @Mock
    private TransactionalOutboxService transactionalOutboxService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private List<FraudRule> mockRules;

    @BeforeEach
    void setUp() {
        mockRules = List.of(
                new FraudRule("RULE_LARGE_AMOUNT", "Large Amount", "Desc",
                        FraudRuleType.AMOUNT_THRESHOLD, 35, new BigDecimal("500000.00"), null, null, true),
                new FraudRule("RULE_CRITICAL_AMOUNT", "Critical Amount", "Desc",
                        FraudRuleType.AMOUNT_THRESHOLD, 85, new BigDecimal("2000000.00"), null, null, true),
                new FraudRule("RULE_VELOCITY_5M", "Velocity Spurt", "Desc",
                        FraudRuleType.VELOCITY, 30, null, 300, 4, true)
        );
        lenient().when(fraudRuleRepository.findByIsActiveTrue()).thenReturn(mockRules);
        lenient().when(fraudAssessmentRepository.save(any(FraudAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Should evaluate normal transaction as LOW risk (Score 0-30, Decision ALLOW)")
    void testLowRiskEvaluation() {
        when(redisVelocityService.getTransactionCountInWindow(anyString(), eq(300))).thenReturn(1);

        EvaluateFraudRequest request = new EvaluateFraudRequest(
                "ACC-1001",
                new BigDecimal("5000.00"),
                "TRANSFER"
        );

        FraudAssessmentResponse response = fraudDetectionService.evaluateTransaction(request, "customer_user");

        assertThat(response).isNotNull();
        assertThat(response.getRiskScore()).isLessThanOrEqualTo(30);
        assertThat(response.getRiskLevel()).isEqualTo(FraudRiskLevel.LOW);
        assertThat(response.getDecision()).isEqualTo(FraudDecision.ALLOW);

        verify(redisVelocityService).recordTransaction(eq("ACC-1001"), eq(new BigDecimal("5000.00")), anyString());
        verify(fraudAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should evaluate high-value transaction as MEDIUM risk when exceeding single threshold")
    void testMediumRiskEvaluation() {
        when(redisVelocityService.getTransactionCountInWindow(anyString(), eq(300))).thenReturn(0);

        EvaluateFraudRequest request = new EvaluateFraudRequest(
                "ACC-1001",
                new BigDecimal("600000.00"), // Exceeds 500k limit (+35 pts)
                "TRANSFER"
        );

        FraudAssessmentResponse response = fraudDetectionService.evaluateTransaction(request, "customer_user");

        assertThat(response.getRiskScore()).isEqualTo(35);
        assertThat(response.getRiskLevel()).isEqualTo(FraudRiskLevel.MEDIUM);
        assertThat(response.getDecision()).isEqualTo(FraudDecision.ALLOW);
        assertThat(response.getTriggeredRuleCodes()).contains("RULE_LARGE_AMOUNT");
    }

    @Test
    @DisplayName("Should evaluate transaction as HIGH risk when velocity spikes (Score 61-80, Decision MANUAL_REVIEW)")
    void testHighRiskVelocitySpurt() {
        // Exceeds 500k (+35 pts) AND velocity >= 4 (+30 pts) = 65 pts -> HIGH
        when(redisVelocityService.getTransactionCountInWindow(anyString(), eq(300))).thenReturn(5);

        EvaluateFraudRequest request = new EvaluateFraudRequest(
                "ACC-1001",
                new BigDecimal("700000.00"),
                "WITHDRAWAL"
        );

        FraudAssessmentResponse response = fraudDetectionService.evaluateTransaction(request, "customer_user");

        assertThat(response.getRiskScore()).isEqualTo(65);
        assertThat(response.getRiskLevel()).isEqualTo(FraudRiskLevel.HIGH);
        assertThat(response.getDecision()).isEqualTo(FraudDecision.MANUAL_REVIEW);

        // Generates alert for manual review and outbox event
        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(transactionalOutboxService, atLeastOnce()).saveEvent(any(), any(), eq("fraud.alert.created"), any(), any());
    }

    @Test
    @DisplayName("Should evaluate transaction as CRITICAL risk when critical threshold breached (Score 85, Decision BLOCK)")
    void testCriticalRiskHardBlock() {
        when(redisVelocityService.getTransactionCountInWindow(anyString(), eq(300))).thenReturn(0);

        EvaluateFraudRequest request = new EvaluateFraudRequest(
                "ACC-1001",
                new BigDecimal("2500000.00"), // Exceeds both 500k and 2M (+35 + 85 = capped at 100)
                "TRANSFER"
        );

        FraudAssessmentResponse response = fraudDetectionService.evaluateTransaction(request, "customer_user");

        assertThat(response.getRiskScore()).isGreaterThanOrEqualTo(81);
        assertThat(response.getRiskLevel()).isEqualTo(FraudRiskLevel.CRITICAL);
        assertThat(response.getDecision()).isEqualTo(FraudDecision.BLOCK);

        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(notificationService).notifyUserWithAction(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should update alert status and record immutable investigation entry")
    void testUpdateAlertStatus() {
        FraudAssessment assessment = new FraudAssessment("ASM-1", "ACC-1001", null, "TRANSFER",
                new BigDecimal("100000.00"), 75, FraudRiskLevel.HIGH, FraudDecision.MANUAL_REVIEW, "RULE", "Reason");
        assessment.setId(10L);

        FraudAlert alert = new FraudAlert("ALT-100", assessment, "ACC-1001", null, "TXN-1",
                75, FraudRiskLevel.HIGH, FraudAlertStatus.PENDING_REVIEW, null);
        alert.setId(5L);

        when(fraudAlertRepository.findById(5L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        UpdateAlertStatusRequest req = new UpdateAlertStatusRequest(
                FraudAlertStatus.RESOLVED_CLEARED,
                "Verified customer identity and transaction authorization via video call",
                "CLEARED_LEGITIMATE"
        );

        FraudAlertResponse response = fraudDetectionService.updateAlertStatus(5L, req, "compliance_officer");

        assertThat(response).isNotNull();
        assertThat(alert.getStatus()).isEqualTo(FraudAlertStatus.RESOLVED_CLEARED);
        assertThat(alert.getAssignedReviewer()).isEqualTo("compliance_officer");

        verify(fraudInvestigationRepository).save(argThat(inv ->
                inv.getInvestigatorUsername().equals("compliance_officer") &&
                inv.getNotes().contains("Verified customer identity")
        ));
    }

    @Test
    @DisplayName("Should aggregate historical account risk profile")
    void testGetAccountRiskProfile() {
        FraudAssessment asm1 = new FraudAssessment("ASM-1", "ACC-1", null, "TRANSFER",
                new BigDecimal("1000.00"), 20, FraudRiskLevel.LOW, FraudDecision.ALLOW, "", "");
        FraudAssessment asm2 = new FraudAssessment("ASM-2", "ACC-1", null, "TRANSFER",
                new BigDecimal("600000.00"), 40, FraudRiskLevel.MEDIUM, FraudDecision.ALLOW, "", "");

        Page<FraudAssessment> asmPage = new PageImpl<>(List.of(asm2, asm1));
        when(fraudAssessmentRepository.findByAccountNumberOrderByEvaluatedAtDesc(eq("ACC-1"), any(PageRequest.class)))
                .thenReturn(asmPage);

        when(fraudAlertRepository.findByAccountNumberOrderByCreatedAtDesc("ACC-1")).thenReturn(List.of());

        AccountRiskProfileResponse profile = fraudDetectionService.getAccountRiskProfile("ACC-1");

        assertThat(profile).isNotNull();
        assertThat(profile.getAccountNumber()).isEqualTo("ACC-1");
        assertThat(profile.getTotalEvaluations()).isEqualTo(2);
        assertThat(profile.getAverageRiskScore()).isEqualTo(30); // (20 + 40) / 2
        assertThat(profile.isFlagged()).isFalse();
    }
}
