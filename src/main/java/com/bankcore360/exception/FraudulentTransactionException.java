package com.bankcore360.exception;

public class FraudulentTransactionException extends RuntimeException {

    private final int riskScore;
    private final String riskLevel;
    private final String assessmentReference;

    public FraudulentTransactionException(String message, int riskScore, String riskLevel, String assessmentReference) {
        super(message);
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.assessmentReference = assessmentReference;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getAssessmentReference() {
        return assessmentReference;
    }
}
