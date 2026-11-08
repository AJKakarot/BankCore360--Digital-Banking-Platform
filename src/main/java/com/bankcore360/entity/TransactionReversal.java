package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "transaction_reversals",
    indexes = {
        @Index(name = "idx_rev_original_tx", columnList = "original_transaction_id", unique = true),
        @Index(name = "idx_rev_reversal_tx", columnList = "reversal_transaction_id", unique = true)
    }
)
public class TransactionReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "original_transaction_id", nullable = false, unique = true)
    private LedgerTransaction originalTransaction;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reversal_transaction_id", nullable = false, unique = true)
    private LedgerTransaction reversalTransaction;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "reversed_by", nullable = false, length = 64)
    private String reversedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TransactionReversal() {
    }

    public TransactionReversal(LedgerTransaction originalTransaction,
                               LedgerTransaction reversalTransaction,
                               String reason, String reversedBy) {
        this.originalTransaction = originalTransaction;
        this.reversalTransaction = reversalTransaction;
        this.reason = reason;
        this.reversedBy = reversedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerTransaction getOriginalTransaction() {
        return originalTransaction;
    }

    public void setOriginalTransaction(LedgerTransaction originalTransaction) {
        this.originalTransaction = originalTransaction;
    }

    public LedgerTransaction getReversalTransaction() {
        return reversalTransaction;
    }

    public void setReversalTransaction(LedgerTransaction reversalTransaction) {
        this.reversalTransaction = reversalTransaction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(String reversedBy) {
        this.reversedBy = reversedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
