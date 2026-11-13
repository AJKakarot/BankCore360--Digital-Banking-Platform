package com.bankcore360.service;

import com.bankcore360.config.KafkaConfig;
import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.exception.FraudAlertNotFoundException;
import com.bankcore360.exception.FraudulentTransactionException;
import com.bankcore360.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudInvestigationRepository fraudInvestigationRepository;
    private final RedisVelocityService redisVelocityService;
    private final TransactionalOutboxService transactionalOutboxService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;

    public FraudDetectionService(FraudRuleRepository fraudRuleRepository,
                                 FraudAssessmentRepository fraudAssessmentRepository,
                                 FraudAlertRepository fraudAlertRepository,
                                 FraudInvestigationRepository fraudInvestigationRepository,
                                 RedisVelocityService redisVelocityService,
                                 TransactionalOutboxService transactionalOutboxService,
                                 NotificationService notificationService,
                                 CustomerRepository customerRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudInvestigationRepository = fraudInvestigationRepository;
        this.redisVelocityService = redisVelocityService;
        this.transactionalOutboxService = transactionalOutboxService;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
    }

    /**
     * Seed baseline rules if table is empty.
     */
    @Transactional
    public void ensureDefaultRulesSeeded() {
        if (fraudRuleRepository.count() == 0) {
            log.info("Seeding baseline FinTech fraud detection rules");
            List<FraudRule> defaultRules = List.of(
                    new FraudRule("RULE_LARGE_AMOUNT", "Unusually Large Transaction",
                            "Transactions exceeding ₹500,000 threshold",
                            FraudRuleType.AMOUNT_THRESHOLD, 35, new BigDecimal("500000.00"), null, null, true),
                    new FraudRule("RULE_CRITICAL_AMOUNT", "Critical High-Value Transfer",
                            "Single transaction exceeding ₹2,000,000 threshold",
                            FraudRuleType.AMOUNT_THRESHOLD, 85, new BigDecimal("2000000.00"), null, null, true),
                    new FraudRule("RULE_VELOCITY_5M", "High Velocity Spurt (5 Minutes)",
                            "More than 4 transactions within 5 minutes window",
                            FraudRuleType.VELOCITY, 30, null, 300, 4, true),
                    new FraudRule("RULE_RAPID_BURST", "Rapid Succession Spikes",
                            "2 or more transactions within 15 seconds burst",
                            FraudRuleType.RAPID_SUCCESSION, 40, null, 15, 2, true),
                    new FraudRule("RULE_VOLUME_HOUR", "Unusual Cumulative Hourly Volume",
                            "Hourly cumulative volume exceeding ₹1,000,000",
                            FraudRuleType.PATTERN_ANOMALY, 35, new BigDecimal("1000000.00"), 3600, null, true)
            );
            fraudRuleRepository.saveAll(defaultRules);
        }
    }

    /**
     * Evaluates a proposed transaction for financial fraud, velocity anomalies, and pattern risks.
     */
    @Transactional
    public FraudAssessmentResponse evaluateTransaction(EvaluateFraudRequest request, String initiatedBy) {
        ensureDefaultRulesSeeded();

        List<FraudRule> activeRules = fraudRuleRepository.findByIsActiveTrue();
        int totalRiskScore = 0;
        List<String> triggeredRuleCodes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        // 1. Evaluate Rule: Single Amount Thresholds
        for (FraudRule rule : activeRules) {
            if (rule.getRuleType() == FraudRuleType.AMOUNT_THRESHOLD && rule.getThresholdValue() != null) {
                if (request.getAmount().compareTo(rule.getThresholdValue()) >= 0) {
                    totalRiskScore += rule.getWeightScore();
                    triggeredRuleCodes.add(rule.getRuleCode());
                    reasons.add(String.format("%s: Amount ₹%s meets/exceeds limit ₹%s (+%d pts)",
                            rule.getName(), request.getAmount(), rule.getThresholdValue(), rule.getWeightScore()));
                }
            }
        }

        // 2. Evaluate Rule: Velocity Checks using Redis
        for (FraudRule rule : activeRules) {
            if (rule.getRuleType() == FraudRuleType.VELOCITY && rule.getTimeWindowSeconds() != null && rule.getMaxAllowedEvents() != null) {
                int count = redisVelocityService.getTransactionCountInWindow(request.getAccountNumber(), rule.getTimeWindowSeconds());
                if (count >= rule.getMaxAllowedEvents()) {
                    totalRiskScore += rule.getWeightScore();
                    triggeredRuleCodes.add(rule.getRuleCode());
                    reasons.add(String.format("%s: Detected %d transactions in %d sec (allowed: %d) (+%d pts)",
                            rule.getName(), count, rule.getTimeWindowSeconds(), rule.getMaxAllowedEvents(), rule.getWeightScore()));
                }
            }
        }

        // 3. Evaluate Rule: Rapid Succession Burst Check
        for (FraudRule rule : activeRules) {
            if (rule.getRuleType() == FraudRuleType.RAPID_SUCCESSION && rule.getTimeWindowSeconds() != null && rule.getMaxAllowedEvents() != null) {
                boolean burst = redisVelocityService.isRapidBurst(request.getAccountNumber(), rule.getTimeWindowSeconds(), rule.getMaxAllowedEvents());
                if (burst) {
                    totalRiskScore += rule.getWeightScore();
                    triggeredRuleCodes.add(rule.getRuleCode());
                    reasons.add(String.format("%s: Multiple transactions within %ds burst (+%d pts)",
                            rule.getName(), rule.getTimeWindowSeconds(), rule.getWeightScore()));
                }
            }
        }

        // 4. Evaluate Rule: Hourly Volume Pattern Anomaly
        for (FraudRule rule : activeRules) {
            if (rule.getRuleType() == FraudRuleType.PATTERN_ANOMALY && rule.getThresholdValue() != null) {
                BigDecimal hourlyVol = redisVelocityService.getVolumeInWindow(request.getAccountNumber(), 3600);
                if (hourlyVol.add(request.getAmount()).compareTo(rule.getThresholdValue()) >= 0) {
                    totalRiskScore += rule.getWeightScore();
                    triggeredRuleCodes.add(rule.getRuleCode());
                    reasons.add(String.format("%s: Cumulative volume ₹%s exceeds hourly limit ₹%s (+%d pts)",
                            rule.getName(), hourlyVol.add(request.getAmount()), rule.getThresholdValue(), rule.getWeightScore()));
                }
            }
        }

        // Cap risk score at 100
        int finalScore = Math.min(100, Math.max(0, totalRiskScore));

        // Determine Risk Classification:
        // 0–30: Low Risk
        // 31–60: Medium Risk
        // 61–80: High Risk
        // 81–100: Critical Risk
        FraudRiskLevel riskLevel;
        FraudDecision decision;

        if (finalScore <= 30) {
            riskLevel = FraudRiskLevel.LOW;
            decision = FraudDecision.ALLOW;
            if (reasons.isEmpty()) reasons.add("Normal transaction behavior. Risk parameters nominal.");
        } else if (finalScore <= 60) {
            riskLevel = FraudRiskLevel.MEDIUM;
            decision = FraudDecision.ALLOW;
            reasons.add("Elevated transaction profile. Approved with telemetry logging.");
        } else if (finalScore <= 80) {
            riskLevel = FraudRiskLevel.HIGH;
            decision = FraudDecision.MANUAL_REVIEW;
            reasons.add("High risk indicators detected. Flagged for compliance manual review.");
        } else {
            riskLevel = FraudRiskLevel.CRITICAL;
            decision = FraudDecision.BLOCK;
            reasons.add("Critical risk violation! Potential account takeover or anomalous transaction pattern.");
        }

        String asmRef = "ASM-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Customer customer = null;
        if (request.getAccountNumber() != null) {
            // Optional customer reference lookup
            customer = customerRepository.findAll().stream().findFirst().orElse(null);
        }

        FraudAssessment assessment = new FraudAssessment(
                asmRef,
                request.getAccountNumber(),
                customer,
                request.getTransactionType(),
                request.getAmount(),
                finalScore,
                riskLevel,
                decision,
                String.join(", ", triggeredRuleCodes),
                String.join(" | ", reasons)
        );
        FraudAssessment savedAssessment = fraudAssessmentRepository.save(assessment);

        // Record event into Redis velocity tracker for ongoing sliding window monitoring
        redisVelocityService.recordTransaction(request.getAccountNumber(), request.getAmount(), asmRef);

        // If High or Critical Risk, generate FraudAlert and dispatch Outbox event
        if (finalScore >= 61) {
            String alertRef = "ALT-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            FraudAlert alert = new FraudAlert(
                    alertRef,
                    savedAssessment,
                    request.getAccountNumber(),
                    customer,
                    null,
                    finalScore,
                    riskLevel,
                    FraudAlertStatus.PENDING_REVIEW,
                    null
            );
            fraudAlertRepository.save(alert);

            // Emit Kafka Event: fraud.alert.created via Transactional Outbox
            transactionalOutboxService.saveEvent(
                    "FRAUD_ALERT",
                    alertRef,
                    "fraud.alert.created",
                    KafkaConfig.TOPIC_FRAUD_ALERT,
                    new FraudAlertResponse(alert)
            );

            // Notify Compliance Officers
            notificationService.notifyUserWithAction(
                    "manager_credit",
                    "SECURITY_ALERT",
                    "🚨 Suspicious Transaction Alert (" + riskLevel + ")",
                    "Account " + request.getAccountNumber() + " attempted transaction of ₹" + request.getAmount() + " with Risk Score: " + finalScore + "/100.",
                    alertRef,
                    "/admin/fraud",
                    "Review Fraud Alert",
                    alertRef
            );
        }

        // Publish Kafka transaction state
        String eventType = (decision == FraudDecision.BLOCK || decision == FraudDecision.MANUAL_REVIEW)
                ? "transaction.flagged" : "transaction.approved";
        String topic = (decision == FraudDecision.BLOCK || decision == FraudDecision.MANUAL_REVIEW)
                ? KafkaConfig.TOPIC_TXN_FLAGGED : KafkaConfig.TOPIC_TXN_APPROVED;

        transactionalOutboxService.saveEvent(
                "TRANSACTION_EVALUATION",
                asmRef,
                eventType,
                topic,
                new FraudAssessmentResponse(savedAssessment)
        );

        log.info("Fraud assessment completed: asmRef={}, account={}, amount={}, score={}, decision={}",
                asmRef, request.getAccountNumber(), request.getAmount(), finalScore, decision);

        return new FraudAssessmentResponse(savedAssessment);
    }

    /**
     * Get paginated list of fraud alerts.
     */
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getAlerts(FraudAlertStatus status, Pageable pageable) {
        Page<FraudAlert> page = (status != null)
                ? fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(alert -> {
            FraudAlertResponse resp = new FraudAlertResponse(alert);
            List<FraudInvestigation> invs = fraudInvestigationRepository.findByAlertIdOrderByCreatedAtDesc(alert.getId());
            resp.setInvestigations(invs.stream().map(FraudInvestigationResponse::new).toList());
            return resp;
        });
    }

    /**
     * Get details of a single fraud alert with investigation history.
     */
    @Transactional(readOnly = true)
    public FraudAlertResponse getAlertById(Long id) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new FraudAlertNotFoundException("Fraud alert not found with id: " + id));
        FraudAlertResponse resp = new FraudAlertResponse(alert);
        List<FraudInvestigation> invs = fraudInvestigationRepository.findByAlertIdOrderByCreatedAtDesc(alert.getId());
        resp.setInvestigations(invs.stream().map(FraudInvestigationResponse::new).toList());
        return resp;
    }

    /**
     * Update the status of a fraud alert and append an immutable investigation record.
     */
    @Transactional
    public FraudAlertResponse updateAlertStatus(Long id, UpdateAlertStatusRequest request, String reviewerUsername) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new FraudAlertNotFoundException("Fraud alert not found with id: " + id));

        alert.setStatus(request.getStatus());
        alert.setAssignedReviewer(reviewerUsername);
        FraudAlert updatedAlert = fraudAlertRepository.save(alert);

        String action = (request.getActionTaken() != null && !request.getActionTaken().isBlank())
                ? request.getActionTaken() : "STATUS_CHANGE_TO_" + request.getStatus();

        FraudInvestigation investigation = new FraudInvestigation(
                updatedAlert,
                reviewerUsername,
                action,
                request.getNotes()
        );
        fraudInvestigationRepository.save(investigation);

        log.info("Fraud alert [{}] updated to [{}] by [{}]", alert.getAlertReference(), request.getStatus(), reviewerUsername);

        FraudAlertResponse resp = new FraudAlertResponse(updatedAlert);
        List<FraudInvestigation> invs = fraudInvestigationRepository.findByAlertIdOrderByCreatedAtDesc(alert.getId());
        resp.setInvestigations(invs.stream().map(FraudInvestigationResponse::new).toList());
        return resp;
    }

    /**
     * Aggregates and returns the historical risk profile of an account.
     */
    @Transactional(readOnly = true)
    public AccountRiskProfileResponse getAccountRiskProfile(String accountNumber) {
        Page<FraudAssessment> assessments = fraudAssessmentRepository.findByAccountNumberOrderByEvaluatedAtDesc(accountNumber, PageRequest.of(0, 50));
        List<FraudAlert> alerts = fraudAlertRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);

        int total = (int) assessments.getTotalElements();
        int avgScore = 0;
        FraudRiskLevel currentLevel = FraudRiskLevel.LOW;
        LocalDateTime lastEvaluatedAt = null;

        if (total > 0) {
            int sum = assessments.getContent().stream().mapToInt(FraudAssessment::getRiskScore).sum();
            avgScore = sum / assessments.getContent().size();
            currentLevel = assessments.getContent().get(0).getRiskLevel();
            lastEvaluatedAt = assessments.getContent().get(0).getEvaluatedAt();
        }

        long activeAlerts = alerts.stream().filter(a -> a.getStatus() == FraudAlertStatus.PENDING_REVIEW || a.getStatus() == FraudAlertStatus.UNDER_INVESTIGATION).count();
        boolean isFlagged = activeAlerts > 0 || currentLevel == FraudRiskLevel.HIGH || currentLevel == FraudRiskLevel.CRITICAL;

        return new AccountRiskProfileResponse(
                accountNumber,
                null,
                "Account " + accountNumber,
                currentLevel,
                avgScore,
                total,
                (int) activeAlerts,
                isFlagged,
                lastEvaluatedAt
        );
    }
}

