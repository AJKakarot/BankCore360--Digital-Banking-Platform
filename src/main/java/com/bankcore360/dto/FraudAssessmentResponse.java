package com.bankcore360.dto;

import com.bankcore360.entity.FraudAssessment;
import com.bankcore360.entity.FraudDecision;
import com.bankcore360.entity.FraudRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FraudAssessmentResponse {

    private Long id;
    private String assessmentReference;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private int riskScore;
    private FraudRiskLevel riskLevel;
    private FraudDecision decision;
    private List<String> triggeredRuleCodes;
    private String assessmentReasons;
    private LocalDateTime evaluatedAt;

    public FraudAssessmentResponse() {
    }

    public FraudAssessmentResponse(FraudAssessment assessment) {
        this.id = assessment.getId();
        this.assessmentReference = assessment.getAssessmentReference();
        this.accountNumber = assessment.getAccountNumber();
        this.transactionType = assessment.getTransactionType();
        this.amount = assessment.getAmount();
        this.riskScore = assessment.getRiskScore();
        this.riskLevel = assessment.getRiskLevel();
        this.decision = assessment.getDecision();
        this.assessmentReasons = assessment.getAssessmentReasons();
        this.evaluatedAt = assessment.getEvaluatedAt();
        if (assessment.getTriggeredRuleCodes() != null && !assessment.getTriggeredRuleCodes().isBlank()) {
            this.triggeredRuleCodes = Arrays.asList(assessment.getTriggeredRuleCodes().split(",\\s*"));
        } else {
            this.triggeredRuleCodes = Collections.emptyList();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssessmentReference() {
        return assessmentReference;
    }

    public void setAssessmentReference(String assessmentReference) {
        this.assessmentReference = assessmentReference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public FraudRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(FraudRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public void setDecision(FraudDecision decision) {
        this.decision = decision;
    }

    public List<String> getTriggeredRuleCodes() {
        return triggeredRuleCodes;
    }

    public void setTriggeredRuleCodes(List<String> triggeredRuleCodes) {
        this.triggeredRuleCodes = triggeredRuleCodes;
    }

    public String getAssessmentReasons() {
        return assessmentReasons;
    }

    public void setAssessmentReasons(String assessmentReasons) {
        this.assessmentReasons = assessmentReasons;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

