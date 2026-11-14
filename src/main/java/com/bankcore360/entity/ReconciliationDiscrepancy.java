package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reconciliation_discrepancies",
    indexes = {
        @Index(name = "idx_rcd_batch", columnList = "batch_id"),
        @Index(name = "idx_rcd_type", columnList = "discrepancy_type"),
        @Index(name = "idx_rcd_status", columnList = "status")
    }
)
public class ReconciliationDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ReconciliationBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_item_id")
    private BankStatementItem statementItem;

    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "ledger_txn_reference", length = 64)
    private String ledgerTxnReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 32)
    private DiscrepancyType discrepancyType;

    @Column(length = 512)
    private String description;

    @Column(name = "statement_amount", precision = 19, scale = 4)
    private BigDecimal statementAmount;

    @Column(name = "ledger_amount", precision = 19, scale = 4)
    private BigDecimal ledgerAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DiscrepancyStatus status = DiscrepancyStatus.OPEN;

    @Column(name = "investigator_notes", length = 1024)
    private String investigatorNotes;

    @Column(name = "resolved_by", length = 64)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReconciliationDiscrepancy() {
    }

    public ReconciliationDiscrepancy(ReconciliationBatch batch, BankStatementItem statementItem,
                                     Long ledgerEntryId, String ledgerTxnReference,
                                     DiscrepancyType discrepancyType, String description,
                                     BigDecimal statementAmount, BigDecimal ledgerAmount) {
        this.batch = batch;
        this.statementItem = statementItem;
        this.ledgerEntryId = ledgerEntryId;
        this.ledgerTxnReference = ledgerTxnReference;
        this.discrepancyType = discrepancyType;
        this.description = description;
        this.statementAmount = statementAmount;
        this.ledgerAmount = ledgerAmount;
        this.status = DiscrepancyStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationBatch getBatch() {
        return batch;
    }

    public void setBatch(ReconciliationBatch batch) {
        this.batch = batch;
    }

    public BankStatementItem getStatementItem() {
        return statementItem;
    }

    public void setStatementItem(BankStatementItem statementItem) {
        this.statementItem = statementItem;
    }

    public Long getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(Long ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

