package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fraud_alerts",
    indexes = {
        @Index(name = "idx_alert_reference", columnList = "alertReference", unique = true),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_account", columnList = "accountNumber"),
        @Index(name = "idx_alert_created_at", columnList = "createdAt")
    }
)
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_reference", nullable = false, unique = true, length = 64)
    private String alertReference;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private FraudAssessment assessment;

    @Column(name = "account_number", nullable = false, length = 36)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private FraudRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudAlertStatus status = FraudAlertStatus.PENDING_REVIEW;

    @Column(name = "assigned_reviewer", length = 64)
    private String assignedReviewer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public FraudAlert() {
    }

    public FraudAlert(String alertReference, FraudAssessment assessment, String accountNumber,
                      Customer customer, String transactionReference, int riskScore,
                      FraudRiskLevel riskLevel, FraudAlertStatus status, String assignedReviewer) {
        this.alertReference = alertReference;
        this.assessment = assessment;
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.transactionReference = transactionReference;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.status = status != null ? status : FraudAlertStatus.PENDING_REVIEW;
        this.assignedReviewer = assignedReviewer;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertReference() {
        return alertReference;
    }

    public void setAlertReference(String alertReference) {
        this.alertReference = alertReference;
    }

    public FraudAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(FraudAssessment assessment) {
        this.assessment = assessment;
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

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
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

    public FraudAlertStatus getStatus() {
        return status;
    }

    public void setStatus(FraudAlertStatus status) {
        this.status = status;
    }

    public String getAssignedReviewer() {
        return assignedReviewer;
    }

    public void setAssignedReviewer(String assignedReviewer) {
        this.assignedReviewer = assignedReviewer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

