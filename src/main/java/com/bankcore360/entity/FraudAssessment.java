package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fraud_assessments",
    indexes = {
        @Index(name = "idx_asm_reference", columnList = "assessmentReference", unique = true),
        @Index(name = "idx_asm_account", columnList = "accountNumber"),
        @Index(name = "idx_asm_risk_level", columnList = "riskLevel"),
        @Index(name = "idx_asm_evaluated_at", columnList = "evaluatedAt")
    }
)
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_reference", nullable = false, unique = true, length = 64)
    private String assessmentReference;

    @Column(name = "account_number", nullable = false, length = 36)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private FraudRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudDecision decision;

    @Column(name = "triggered_rule_codes", columnDefinition = "TEXT")
    private String triggeredRuleCodes;

    @Column(name = "assessment_reasons", columnDefinition = "TEXT")
    private String assessmentReasons;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public FraudAssessment() {
    }

    public FraudAssessment(String assessmentReference, String accountNumber, Customer customer,
                           String transactionType, BigDecimal amount, int riskScore,
                           FraudRiskLevel riskLevel, FraudDecision decision,
                           String triggeredRuleCodes, String assessmentReasons) {
        this.assessmentReference = assessmentReference;
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.transactionType = transactionType;
        this.amount = amount;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.triggeredRuleCodes = triggeredRuleCodes;
        this.assessmentReasons = assessmentReasons;
        this.evaluatedAt = LocalDateTime.now();
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public String getTriggeredRuleCodes() {
        return triggeredRuleCodes;
    }

    public void setTriggeredRuleCodes(String triggeredRuleCodes) {
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
