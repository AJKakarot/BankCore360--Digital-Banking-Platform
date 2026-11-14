package com.bankcore360.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationBatchReport {

    private Long batchId;
    private String batchReference;
    private String statementReference;
    private String bankName;
    private String accountNumber;
    private LocalDateTime reconciledAt;
    private String reconciledBy;
    private int matchedCount;
    private int unmatchedStatementCount;
    private int unmatchedLedgerCount;
    private int discrepancyCount;
    private double matchRatePercentage;
    private String status;
    private List<DiscrepancyItemDto> discrepancies = new ArrayList<>();

    public ReconciliationBatchReport() {
    }

    public ReconciliationBatchReport(Long batchId, String batchReference, String statementReference,
                                     String bankName, String accountNumber, LocalDateTime reconciledAt,
                                     String reconciledBy, int matchedCount, int unmatchedStatementCount,
                                     int unmatchedLedgerCount, int discrepancyCount,
                                     double matchRatePercentage, String status) {
        this.batchId = batchId;
        this.batchReference = batchReference;
        this.statementReference = statementReference;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.reconciledAt = reconciledAt;
        this.reconciledBy = reconciledBy;
        this.matchedCount = matchedCount;
        this.unmatchedStatementCount = unmatchedStatementCount;
        this.unmatchedLedgerCount = unmatchedLedgerCount;
        this.discrepancyCount = discrepancyCount;
        this.matchRatePercentage = matchRatePercentage;
        this.status = status;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public String getStatementReference() {
        return statementReference;
    }

    public void setStatementReference(String statementReference) {
        this.statementReference = statementReference;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(String reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public int getUnmatchedStatementCount() {
        return unmatchedStatementCount;
    }

    public void setUnmatchedStatementCount(int unmatchedStatementCount) {
        this.unmatchedStatementCount = unmatchedStatementCount;
    }

    public int getUnmatchedLedgerCount() {
        return unmatchedLedgerCount;
    }

    public void setUnmatchedLedgerCount(int unmatchedLedgerCount) {
        this.unmatchedLedgerCount = unmatchedLedgerCount;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(int discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public double getMatchRatePercentage() {
        return matchRatePercentage;
    }

    public void setMatchRatePercentage(double matchRatePercentage) {
        this.matchRatePercentage = matchRatePercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DiscrepancyItemDto> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<DiscrepancyItemDto> discrepancies) {
        this.discrepancies = discrepancies;
    }
}

