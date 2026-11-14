package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "reconciliation_batches",
    indexes = {
        @Index(name = "idx_rcb_ref", columnList = "batch_reference", unique = true),
        @Index(name = "idx_rcb_stmt", columnList = "statement_id")
    }
)
public class ReconciliationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 64)
    private String batchReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private BankStatement statement;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt = LocalDateTime.now();

    @Column(name = "reconciled_by", length = 64)
    private String reconciledBy;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount = 0;

    @Column(name = "unmatched_statement_count", nullable = false)
    private int unmatchedStatementCount = 0;

    @Column(name = "unmatched_ledger_count", nullable = false)
    private int unmatchedLedgerCount = 0;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount = 0;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "COMPLETED";

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

    public ReconciliationBatch() {
    }

    public ReconciliationBatch(String batchReference, BankStatement statement, String reconciledBy) {
        this.batchReference = batchReference;
        this.statement = statement;
        this.reconciledBy = reconciledBy;
        this.reconciledAt = LocalDateTime.now();
        this.status = "COMPLETED";
    }

    public void addDiscrepancy(ReconciliationDiscrepancy discrepancy) {
        discrepancies.add(discrepancy);
        discrepancy.setBatch(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public BankStatement getStatement() {
        return statement;
    }

    public void setStatement(BankStatement statement) {
        this.statement = statement;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ReconciliationDiscrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<ReconciliationDiscrepancy> discrepancies) {
        this.discrepancies = discrepancies;
    }
}

