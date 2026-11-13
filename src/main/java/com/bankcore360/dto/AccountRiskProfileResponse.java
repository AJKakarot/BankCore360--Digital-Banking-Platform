package com.bankcore360.dto;

import com.bankcore360.entity.FraudRiskLevel;

import java.time.LocalDateTime;

public class AccountRiskProfileResponse {

    private String accountNumber;
    private Long customerId;
    private String customerName;
    private FraudRiskLevel currentRiskLevel;
    private int averageRiskScore;
    private int totalEvaluations;
    private int activeAlertsCount;
    private boolean isFlagged;
    private LocalDateTime lastEvaluatedAt;

    public AccountRiskProfileResponse() {
    }

    public AccountRiskProfileResponse(String accountNumber, Long customerId, String customerName,
                                      FraudRiskLevel currentRiskLevel, int averageRiskScore,
                                      int totalEvaluations, int activeAlertsCount,
                                      boolean isFlagged, LocalDateTime lastEvaluatedAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.currentRiskLevel = currentRiskLevel;
        this.averageRiskScore = averageRiskScore;
        this.totalEvaluations = totalEvaluations;
        this.activeAlertsCount = activeAlertsCount;
        this.isFlagged = isFlagged;
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public FraudRiskLevel getCurrentRiskLevel() {
        return currentRiskLevel;
    }

    public void setCurrentRiskLevel(FraudRiskLevel currentRiskLevel) {
        this.currentRiskLevel = currentRiskLevel;
    }

    public int getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(int averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public int getTotalEvaluations() {
        return totalEvaluations;
    }

    public void setTotalEvaluations(int totalEvaluations) {
        this.totalEvaluations = totalEvaluations;
    }

    public int getActiveAlertsCount() {
        return activeAlertsCount;
    }

    public void setActiveAlertsCount(int activeAlertsCount) {
        this.activeAlertsCount = activeAlertsCount;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }
}

