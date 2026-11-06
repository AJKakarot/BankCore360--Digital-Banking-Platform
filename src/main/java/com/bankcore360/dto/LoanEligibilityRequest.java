package com.bankcore360.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanEligibilityRequest {

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "5000.00", message = "Monthly income must be at least 5,000.00")
    private BigDecimal monthlyIncome;

    @DecimalMin(value = "0.00", message = "Existing monthly EMI cannot be negative")
    private BigDecimal existingMonthlyEmi = BigDecimal.ZERO;

    @NotNull(message = "Requested loan amount is required")
    @DecimalMin(value = "1000.00", message = "Loan amount must be at least 1,000.00")
    private BigDecimal requestedAmount;

    @NotNull(message = "Tenure in months is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer tenureMonths;

    @NotNull(message = "Annual interest rate is required")
    @DecimalMin(value = "1.00", message = "Annual interest rate must be at least 1.0%")
    @DecimalMax(value = "50.00", message = "Annual interest rate cannot exceed 50.0%")
    private BigDecimal annualInterestRate;

    public LoanEligibilityRequest() {
    }

    public LoanEligibilityRequest(BigDecimal monthlyIncome, BigDecimal existingMonthlyEmi,
                                  BigDecimal requestedAmount, Integer tenureMonths,
                                  BigDecimal annualInterestRate) {
        this.monthlyIncome = monthlyIncome;
        this.existingMonthlyEmi = existingMonthlyEmi != null ? existingMonthlyEmi : BigDecimal.ZERO;
        this.requestedAmount = requestedAmount;
        this.tenureMonths = tenureMonths;
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getExistingMonthlyEmi() {
        return existingMonthlyEmi;
    }

    public void setExistingMonthlyEmi(BigDecimal existingMonthlyEmi) {
        this.existingMonthlyEmi = existingMonthlyEmi != null ? existingMonthlyEmi : BigDecimal.ZERO;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }
}

