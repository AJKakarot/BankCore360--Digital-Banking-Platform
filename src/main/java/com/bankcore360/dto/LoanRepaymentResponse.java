package com.bankcore360.dto;

import com.bankcore360.entity.RepaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanRepaymentResponse {

    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal outstandingBalance;
    private RepaymentStatus status;
    private LocalDateTime paidAt;
    private String paymentReference;
    private BigDecimal latePenaltyAmount = BigDecimal.ZERO;
    private BigDecimal totalPayableAmount;
    private String applicationReference;
    private boolean isOverdue;

    public LoanRepaymentResponse() {
    }

    public LoanRepaymentResponse(Long id, Integer installmentNumber, LocalDate dueDate,
                                 BigDecimal emiAmount, BigDecimal principalComponent,
                                 BigDecimal interestComponent, BigDecimal outstandingBalance,
                                 RepaymentStatus status, LocalDateTime paidAt, String paymentReference) {
        this(id, installmentNumber, dueDate, emiAmount, principalComponent, interestComponent,
             outstandingBalance, status, paidAt, paymentReference, BigDecimal.ZERO, null);
    }

    public LoanRepaymentResponse(Long id, Integer installmentNumber, LocalDate dueDate,
                                 BigDecimal emiAmount, BigDecimal principalComponent,
                                 BigDecimal interestComponent, BigDecimal outstandingBalance,
                                 RepaymentStatus status, LocalDateTime paidAt, String paymentReference,
                                 BigDecimal latePenaltyAmount, String applicationReference) {
        this.id = id;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.emiAmount = emiAmount;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.latePenaltyAmount = latePenaltyAmount != null ? latePenaltyAmount : BigDecimal.ZERO;
        this.totalPayableAmount = (emiAmount != null ? emiAmount : BigDecimal.ZERO)
                .add(this.latePenaltyAmount);
        this.applicationReference = applicationReference;
        this.isOverdue = status == RepaymentStatus.OVERDUE ||
                (status == RepaymentStatus.UPCOMING && dueDate != null && dueDate.isBefore(LocalDate.now()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public BigDecimal getPrincipalComponent() {
        return principalComponent;
    }

    public void setPrincipalComponent(BigDecimal principalComponent) {
        this.principalComponent = principalComponent;
    }

    public BigDecimal getInterestComponent() {
        return interestComponent;
    }

    public void setInterestComponent(BigDecimal interestComponent) {
        this.interestComponent = interestComponent;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public RepaymentStatus getStatus() {
        return status;
    }

    public void setStatus(RepaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getLatePenaltyAmount() {
        return latePenaltyAmount;
    }

    public void setLatePenaltyAmount(BigDecimal latePenaltyAmount) {
        this.latePenaltyAmount = latePenaltyAmount;
        if (this.emiAmount != null) {
            this.totalPayableAmount = this.emiAmount.add(this.latePenaltyAmount != null ? this.latePenaltyAmount : BigDecimal.ZERO);
        }
    }

    public BigDecimal getTotalPayableAmount() {
        return totalPayableAmount != null ? totalPayableAmount : emiAmount;
    }

    public void setTotalPayableAmount(BigDecimal totalPayableAmount) {
        this.totalPayableAmount = totalPayableAmount;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public boolean isOverdue() {
        return isOverdue;
    }

    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }
}
