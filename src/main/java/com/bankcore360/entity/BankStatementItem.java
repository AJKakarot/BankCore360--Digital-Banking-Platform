package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bank_statement_items",
    indexes = {
        @Index(name = "idx_sitem_stmt", columnList = "statement_id"),
        @Index(name = "idx_sitem_ref", columnList = "item_reference"),
        @Index(name = "idx_sitem_ext_ref", columnList = "external_reference"),
        @Index(name = "idx_sitem_status", columnList = "reconciliation_status")
    }
)
public class BankStatementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private BankStatement statement;

    @Column(name = "item_reference", nullable = false, length = 64)
    private String itemReference;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerEntryDirection direction;

    @Column(length = 255)
    private String description;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 32)
    private ReconciliationItemStatus reconciliationStatus = ReconciliationItemStatus.UNMATCHED;

    @Column(name = "matched_ledger_entry_id")
    private Long matchedLedgerEntryId;

    @Column(name = "matched_ledger_txn_ref", length = 64)
    private String matchedLedgerTxnRef;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    public BankStatementItem() {
    }

    public BankStatementItem(String itemReference, LocalDate transactionDate, BigDecimal amount,
                             LedgerEntryDirection direction, String description, String externalReference) {
        this.itemReference = itemReference;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.direction = direction;
        this.description = description;
        this.externalReference = externalReference;
        this.reconciliationStatus = ReconciliationItemStatus.UNMATCHED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankStatement getStatement() {
        return statement;
    }

    public void setStatement(BankStatement statement) {
        this.statement = statement;
    }

    public String getItemReference() {
        return itemReference;
    }

    public void setItemReference(String itemReference) {
        this.itemReference = itemReference;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LedgerEntryDirection getDirection() {
        return direction;
    }

    public void setDirection(LedgerEntryDirection direction) {
        this.direction = direction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public ReconciliationItemStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(ReconciliationItemStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public Long getMatchedLedgerEntryId() {
        return matchedLedgerEntryId;
    }

    public void setMatchedLedgerEntryId(Long matchedLedgerEntryId) {
        this.matchedLedgerEntryId = matchedLedgerEntryId;
    }

    public String getMatchedLedgerTxnRef() {
        return matchedLedgerTxnRef;
    }

    public void setMatchedLedgerTxnRef(String matchedLedgerTxnRef) {
        this.matchedLedgerTxnRef = matchedLedgerTxnRef;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }
}
