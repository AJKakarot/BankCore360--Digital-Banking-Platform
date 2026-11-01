package com.bankcore360.dto;

import java.math.BigDecimal;
import java.util.Map;

public class LoanAnalyticsResponse {

    private long totalCustomers;
    private long totalApplications;
    private long pendingCheckerReviews;
    private long pendingManagerApprovals;
    private long approvedLoans;
    private long rejectedLoans;
    private long disbursedLoans;
    private BigDecimal totalApprovedLoanAmount;
    private BigDecimal totalDisbursedLoanAmount;
    private Map<String, Long> statusDistribution;
    private Map<String, Long> loanTypeDistribution;

    public LoanAnalyticsResponse() {
    }

    public LoanAnalyticsResponse(long totalCustomers, long totalApplications, long pendingCheckerReviews,
                                 long pendingManagerApprovals, long approvedLoans, long rejectedLoans,
                                 long disbursedLoans, BigDecimal totalApprovedLoanAmount,
                                 BigDecimal totalDisbursedLoanAmount, Map<String, Long> statusDistribution,
                                 Map<String, Long> loanTypeDistribution) {
        this.totalCustomers = totalCustomers;
        this.totalApplications = totalApplications;
        this.pendingCheckerReviews = pendingCheckerReviews;
        this.pendingManagerApprovals = pendingManagerApprovals;
        this.approvedLoans = approvedLoans;
        this.rejectedLoans = rejectedLoans;
        this.disbursedLoans = disbursedLoans;
        this.totalApprovedLoanAmount = totalApprovedLoanAmount;
        this.totalDisbursedLoanAmount = totalDisbursedLoanAmount;
        this.statusDistribution = statusDistribution;
        this.loanTypeDistribution = loanTypeDistribution;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getPendingCheckerReviews() {
        return pendingCheckerReviews;
    }

    public void setPendingCheckerReviews(long pendingCheckerReviews) {
        this.pendingCheckerReviews = pendingCheckerReviews;
    }

    public long getPendingManagerApprovals() {
        return pendingManagerApprovals;
    }

    public void setPendingManagerApprovals(long pendingManagerApprovals) {
        this.pendingManagerApprovals = pendingManagerApprovals;
    }

    public long getApprovedLoans() {
        return approvedLoans;
    }

    public void setApprovedLoans(long approvedLoans) {
        this.approvedLoans = approvedLoans;
    }

    public long getRejectedLoans() {
        return rejectedLoans;
    }

    public void setRejectedLoans(long rejectedLoans) {
        this.rejectedLoans = rejectedLoans;
    }

    public long getDisbursedLoans() {
        return disbursedLoans;
    }

    public void setDisbursedLoans(long disbursedLoans) {
        this.disbursedLoans = disbursedLoans;
    }

    public BigDecimal getTotalApprovedLoanAmount() {
        return totalApprovedLoanAmount;
    }

    public void setTotalApprovedLoanAmount(BigDecimal totalApprovedLoanAmount) {
        this.totalApprovedLoanAmount = totalApprovedLoanAmount;
    }

    public BigDecimal getTotalDisbursedLoanAmount() {
        return totalDisbursedLoanAmount;
    }

    public void setTotalDisbursedLoanAmount(BigDecimal totalDisbursedLoanAmount) {
        this.totalDisbursedLoanAmount = totalDisbursedLoanAmount;
    }

    public Map<String, Long> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<String, Long> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public Map<String, Long> getLoanTypeDistribution() {
        return loanTypeDistribution;
    }

    public void setLoanTypeDistribution(Map<String, Long> loanTypeDistribution) {
        this.loanTypeDistribution = loanTypeDistribution;
    }
}

