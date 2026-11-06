package com.bankcore360.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ManagerDecisionRequest {

    @DecimalMin(value = "1000.00", message = "Approved amount must be at least 1,000.00")
    private BigDecimal approvedAmount;

    @DecimalMin(value = "1.00", message = "Interest rate must be at least 1.0%")
    @DecimalMax(value = "40.00", message = "Interest rate cannot exceed 40.0%")
    private BigDecimal interestRate;

    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer approvedTenureMonths;

    @Size(max = 2000, message = "Remarks cannot exceed 2000 characters")
    private String managerRemarks;

    @Size(max = 2000, message = "Rejection reason cannot exceed 2000 characters")
    private String rejectionReason;

    public ManagerDecisionRequest() {
    }

    public ManagerDecisionRequest(BigDecimal approvedAmount, BigDecimal interestRate,
                                  Integer approvedTenureMonths, String managerRemarks) {
        this.approvedAmount = approvedAmount;
        this.interestRate = interestRate;
        this.approvedTenureMonths = approvedTenureMonths;
        this.managerRemarks = managerRemarks;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getApprovedTenureMonths() {
        return approvedTenureMonths;
    }

    public void setApprovedTenureMonths(Integer approvedTenureMonths) {
        this.approvedTenureMonths = approvedTenureMonths;
    }

    public String getManagerRemarks() {
        return managerRemarks;
    }

    public void setManagerRemarks(String managerRemarks) {
        this.managerRemarks = managerRemarks;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

