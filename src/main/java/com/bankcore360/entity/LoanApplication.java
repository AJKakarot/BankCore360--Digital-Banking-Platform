package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a Customer Loan Application in the Maker-Checker-Manager workflow.
 */
@Entity
@Table(name = "loan_applications", indexes = {
        @Index(name = "idx_loan_app_ref", columnList = "applicationReference", unique = true),
        @Index(name = "idx_loan_app_customer", columnList = "customer_id"),
        @Index(name = "idx_loan_app_status", columnList = "status")
})
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String applicationReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoanType loanType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false, length = 1000)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoanStatus status = LoanStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Manager approval parameters
    @Column(precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(precision = 7, scale = 4)
    private BigDecimal interestRate;

    private Integer approvedTenureMonths;

    @Column(length = 2000)
    private String managerRemarks;

    private String reviewedBy;

    private LocalDateTime decisionTimestamp;

    // Disbursement parameters
    private LocalDateTime disbursedAt;

    @Column(length = 64)
    private String disbursementReference;

    private String disbursedBy;

    // Rejection details
    @Column(length = 2000)
    private String rejectionReason;

    private String rejectedBy;

    private LocalDateTime rejectedAt;

    public LoanApplication() {
    }

    public LoanApplication(String applicationReference, Customer customer, LoanType loanType,
                           BigDecimal requestedAmount, Integer tenureMonths, String purpose) {
        this.applicationReference = applicationReference;
        this.customer = customer;
        this.loanType = loanType;
        this.requestedAmount = requestedAmount;
        this.tenureMonths = tenureMonths;
        this.purpose = purpose;
        this.status = LoanStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getDecisionTimestamp() {
        return decisionTimestamp;
    }

    public void setDecisionTimestamp(LocalDateTime decisionTimestamp) {
        this.decisionTimestamp = decisionTimestamp;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
    }

    public String getDisbursedBy() {
        return disbursedBy;
    }

    public void setDisbursedBy(String disbursedBy) {
        this.disbursedBy = disbursedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
}

