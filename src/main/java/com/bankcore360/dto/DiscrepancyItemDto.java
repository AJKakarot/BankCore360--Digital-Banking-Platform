package com.bankcore360.dto;

import com.bankcore360.entity.DiscrepancyStatus;
import com.bankcore360.entity.DiscrepancyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiscrepancyItemDto {

    private Long id;
    private String statementItemReference;
    private String ledgerTxnReference;
    private DiscrepancyType discrepancyType;
    private String description;
    private BigDecimal statementAmount;
    private BigDecimal ledgerAmount;
    private BigDecimal variance;
    private DiscrepancyStatus status;
    private String investigatorNotes;
    private String resolvedBy;
    private LocalDateTime resolvedAt;

    public DiscrepancyItemDto() {
    }

    public DiscrepancyItemDto(Long id, String statementItemReference, String ledgerTxnReference,
                              DiscrepancyType discrepancyType, String description,
                              BigDecimal statementAmount, BigDecimal ledgerAmount,
                              BigDecimal variance, DiscrepancyStatus status,
                              String investigatorNotes, String resolvedBy, LocalDateTime resolvedAt) {
        this.id = id;
        this.statementItemReference = statementItemReference;
        this.ledgerTxnReference = ledgerTxnReference;
        this.discrepancyType = discrepancyType;
        this.description = description;
        this.statementAmount = statementAmount;
        this.ledgerAmount = ledgerAmount;
        this.variance = variance;
        this.status = status;
        this.investigatorNotes = investigatorNotes;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatementItemReference() {
        return statementItemReference;
    }

    public void setStatementItemReference(String statementItemReference) {
        this.statementItemReference = statementItemReference;
    }

    public String getLedgerTxnReference() {
        return ledgerTxnReference;
    }

    public void setLedgerTxnReference(String ledgerTxnReference) {
        this.ledgerTxnReference = ledgerTxnReference;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public void setDiscrepancyType(DiscrepancyType discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStatementAmount() {
        return statementAmount;
    }

    public void setStatementAmount(BigDecimal statementAmount) {
        this.statementAmount = statementAmount;
    }

    public BigDecimal getLedgerAmount() {
        return ledgerAmount;
    }

    public void setLedgerAmount(BigDecimal ledgerAmount) {
        this.ledgerAmount = ledgerAmount;
    }

    public BigDecimal getVariance() {
        return variance;
    }

    public void setVariance(BigDecimal variance) {
        this.variance = variance;
    }

    public DiscrepancyStatus getStatus() {
        return status;
    }

    public void setStatus(DiscrepancyStatus status) {
        this.status = status;
    }

    public String getInvestigatorNotes() {
        return investigatorNotes;
    }

    public void setInvestigatorNotes(String investigatorNotes) {
        this.investigatorNotes = investigatorNotes;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
