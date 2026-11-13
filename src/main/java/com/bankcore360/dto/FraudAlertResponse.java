package com.bankcore360.dto;

import com.bankcore360.entity.FraudAlert;
import com.bankcore360.entity.FraudAlertStatus;
import com.bankcore360.entity.FraudRiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public class FraudAlertResponse {

    private Long id;
    private String alertReference;
    private Long assessmentId;
    private String assessmentReference;
    private String accountNumber;
    private String transactionReference;
    private int riskScore;
    private FraudRiskLevel riskLevel;
    private FraudAlertStatus status;
    private String assignedReviewer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private FraudAssessmentResponse assessmentDetails;
    private List<FraudInvestigationResponse> investigations;

    public FraudAlertResponse() {
    }

    public FraudAlertResponse(FraudAlert alert) {
        this.id = alert.getId();
        this.alertReference = alert.getAlertReference();
        if (alert.getAssessment() != null) {
            this.assessmentId = alert.getAssessment().getId();
            this.assessmentReference = alert.getAssessment().getAssessmentReference();
            this.assessmentDetails = new FraudAssessmentResponse(alert.getAssessment());
        }
        this.accountNumber = alert.getAccountNumber();
        this.transactionReference = alert.getTransactionReference();
        this.riskScore = alert.getRiskScore();
        this.riskLevel = alert.getRiskLevel();
        this.status = alert.getStatus();
        this.assignedReviewer = alert.getAssignedReviewer();
        this.createdAt = alert.getCreatedAt();
        this.updatedAt = alert.getUpdatedAt();
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

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public FraudAssessmentResponse getAssessmentDetails() {
        return assessmentDetails;
    }

    public void setAssessmentDetails(FraudAssessmentResponse assessmentDetails) {
        this.assessmentDetails = assessmentDetails;
    }

    public List<FraudInvestigationResponse> getInvestigations() {
        return investigations;
    }

    public void setInvestigations(List<FraudInvestigationResponse> investigations) {
        this.investigations = investigations;
    }
}

