package com.bankcore360.dto.internal;

import java.time.LocalDateTime;
import java.util.List;

public class InternalCustomerFraudMetadataDto {
    private String customerUsername;
    private int averageRiskScore;
    private String overallRiskLevel;
    private int totalAlertsCount;
    private int activeAlertsCount;
    private List<FraudAssessmentSummaryDto> recentAssessments;

    public static class FraudAssessmentSummaryDto {
        private String assessmentReference;
        private String transactionReference;
        private int riskScore;
        private String riskLevel;
        private String decision;
        private List<String> triggeredRules;
        private LocalDateTime evaluatedAt;

        public FraudAssessmentSummaryDto() {}

        public FraudAssessmentSummaryDto(String assessmentReference, String transactionReference, int riskScore,
                                         String riskLevel, String decision, List<String> triggeredRules, LocalDateTime evaluatedAt) {
            this.assessmentReference = assessmentReference;
            this.transactionReference = transactionReference;
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.decision = decision;
            this.triggeredRules = triggeredRules;
            this.evaluatedAt = evaluatedAt;
        }

        public String getAssessmentReference() { return assessmentReference; }
        public void setAssessmentReference(String assessmentReference) { this.assessmentReference = assessmentReference; }
        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public List<String> getTriggeredRules() { return triggeredRules; }
        public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }
        public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
        public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    }

    public InternalCustomerFraudMetadataDto() {}

    public InternalCustomerFraudMetadataDto(String customerUsername, int averageRiskScore, String overallRiskLevel,
                                            int totalAlertsCount, int activeAlertsCount,
                                            List<FraudAssessmentSummaryDto> recentAssessments) {
        this.customerUsername = customerUsername;
        this.averageRiskScore = averageRiskScore;
        this.overallRiskLevel = overallRiskLevel;
        this.totalAlertsCount = totalAlertsCount;
        this.activeAlertsCount = activeAlertsCount;
        this.recentAssessments = recentAssessments;
    }

    public String getCustomerUsername() { return customerUsername; }
    public void setCustomerUsername(String customerUsername) { this.customerUsername = customerUsername; }
    public int getAverageRiskScore() { return averageRiskScore; }
    public void setAverageRiskScore(int averageRiskScore) { this.averageRiskScore = averageRiskScore; }
    public String getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(String overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; }
    public int getTotalAlertsCount() { return totalAlertsCount; }
    public void setTotalAlertsCount(int totalAlertsCount) { this.totalAlertsCount = totalAlertsCount; }
    public int getActiveAlertsCount() { return activeAlertsCount; }
    public void setActiveAlertsCount(int activeAlertsCount) { this.activeAlertsCount = activeAlertsCount; }
    public List<FraudAssessmentSummaryDto> getRecentAssessments() { return recentAssessments; }
    public void setRecentAssessments(List<FraudAssessmentSummaryDto> recentAssessments) { this.recentAssessments = recentAssessments; }
}
