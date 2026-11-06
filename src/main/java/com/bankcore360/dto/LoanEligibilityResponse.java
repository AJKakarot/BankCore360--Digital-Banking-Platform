package com.bankcore360.dto;

import java.math.BigDecimal;

public class LoanEligibilityResponse {

    private BigDecimal estimatedMonthlyEmi;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayment;
    private BigDecimal monthlyEmiToIncomeRatio; // FOIR (Fixed Obligation to Income Ratio) percentage
    private boolean eligible;
    private BigDecimal maxEligibleLoanAmount;
    private String statusMessage;
    private String disclaimer = "Indicative financial estimate for educational purposes only. Does not guarantee credit approval.";

    public LoanEligibilityResponse() {
    }

    public LoanEligibilityResponse(BigDecimal estimatedMonthlyEmi, BigDecimal totalInterest,
                                   BigDecimal totalRepayment, BigDecimal monthlyEmiToIncomeRatio,
                                   boolean eligible, BigDecimal maxEligibleLoanAmount,
                                   String statusMessage) {
        this.estimatedMonthlyEmi = estimatedMonthlyEmi;
        this.totalInterest = totalInterest;
        this.totalRepayment = totalRepayment;
        this.monthlyEmiToIncomeRatio = monthlyEmiToIncomeRatio;
        this.eligible = eligible;
        this.maxEligibleLoanAmount = maxEligibleLoanAmount;
        this.statusMessage = statusMessage;
    }

    public BigDecimal getEstimatedMonthlyEmi() {
        return estimatedMonthlyEmi;
    }

    public void setEstimatedMonthlyEmi(BigDecimal estimatedMonthlyEmi) {
        this.estimatedMonthlyEmi = estimatedMonthlyEmi;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalRepayment() {
        return totalRepayment;
    }

    public void setTotalRepayment(BigDecimal totalRepayment) {
        this.totalRepayment = totalRepayment;
    }

    public BigDecimal getMonthlyEmiToIncomeRatio() {
        return monthlyEmiToIncomeRatio;
    }

    public void setMonthlyEmiToIncomeRatio(BigDecimal monthlyEmiToIncomeRatio) {
        this.monthlyEmiToIncomeRatio = monthlyEmiToIncomeRatio;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public BigDecimal getMaxEligibleLoanAmount() {
        return maxEligibleLoanAmount;
    }

    public void setMaxEligibleLoanAmount(BigDecimal maxEligibleLoanAmount) {
        this.maxEligibleLoanAmount = maxEligibleLoanAmount;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}

