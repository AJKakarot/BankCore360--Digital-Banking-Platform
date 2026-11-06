package com.bankcore360.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisbursementResponse {

    private String applicationReference;
    private String disbursementReference;
    private BigDecimal disbursedAmount;
    private LocalDateTime disbursedAt;
    private String disbursedBy;
    private String simulatedBeneficiaryAccount;
    private String status;
    private String educationalNotice = "Simulated disbursement for educational banking platform. No actual fund transfer occurred.";

    public DisbursementResponse() {
    }

    public DisbursementResponse(String applicationReference, String disbursementReference,
                                BigDecimal disbursedAmount, LocalDateTime disbursedAt,
                                String disbursedBy, String simulatedBeneficiaryAccount, String status) {
        this.applicationReference = applicationReference;
        this.disbursementReference = disbursementReference;
        this.disbursedAmount = disbursedAmount;
        this.disbursedAt = disbursedAt;
        this.disbursedBy = disbursedBy;
        this.simulatedBeneficiaryAccount = simulatedBeneficiaryAccount;
        this.status = status;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public String getDisbursedBy() {
        return disbursedBy;
    }

    public void setDisbursedBy(String disbursedBy) {
        this.disbursedBy = disbursedBy;
    }

    public String getSimulatedBeneficiaryAccount() {
        return simulatedBeneficiaryAccount;
    }

    public void setSimulatedBeneficiaryAccount(String simulatedBeneficiaryAccount) {
        this.simulatedBeneficiaryAccount = simulatedBeneficiaryAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEducationalNotice() {
        return educationalNotice;
    }

    public void setEducationalNotice(String educationalNotice) {
        this.educationalNotice = educationalNotice;
    }
}

