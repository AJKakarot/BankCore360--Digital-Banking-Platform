package com.bankcore360.dto;

import com.bankcore360.entity.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class LoanApplicationRequest {

    @NotNull(message = "Loan type is required (PERSONAL, HOME, EDUCATION, BUSINESS)")
    private LoanType loanType;

    @NotNull(message = "Requested loan amount is required")
    @DecimalMin(value = "1000.00", message = "Requested loan amount must be at least 1,000.00")
    private BigDecimal requestedAmount;

    @NotNull(message = "Loan tenure in months is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months (30 years)")
    private Integer tenureMonths;

    @NotBlank(message = "Purpose of loan is required")
    @Size(min = 5, max = 1000, message = "Purpose must be between 5 and 1000 characters")
    private String purpose;

    public LoanApplicationRequest() {
    }

    public LoanApplicationRequest(LoanType loanType, BigDecimal requestedAmount, Integer tenureMonths, String purpose) {
        this.loanType = loanType;
        this.requestedAmount = requestedAmount;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

