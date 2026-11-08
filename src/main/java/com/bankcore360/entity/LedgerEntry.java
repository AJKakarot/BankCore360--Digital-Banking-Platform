package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_entry_reference", columnList = "entryReference", unique = true),
        @Index(name = "idx_entry_account", columnList = "account_id"),
        @Index(name = "idx_entry_transaction", columnList = "transaction_id"),
        @Index(name = "idx_entry_created_at", columnList = "createdAt")
    }
)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String entryReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private LedgerTransaction transaction;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private LedgerAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private LedgerEntryDirection direction;

    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "entry_order", nullable = false, updatable = false)
    private int entryOrder;

    @Column(length = 255, updatable = false)
    private String narration;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LedgerEntry() {
    }

    public LedgerEntry(String entryReference, LedgerAccount account,
                       LedgerEntryDirection direction, BigDecimal amount,
                       BigDecimal balanceAfter, int entryOrder, String narration) {
        this.entryReference = entryReference;
        this.account = account;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.entryOrder = entryOrder;
        this.narration = narration;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntryReference() {
        return entryReference;
    }

    public void setEntryReference(String entryReference) {
        this.entryReference = entryReference;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(LedgerTransaction transaction) {
        this.transaction = transaction;
    }

    public LedgerAccount getAccount() {
        return account;
    }

    public void setAccount(LedgerAccount account) {
        this.account = account;
    }

    public LedgerEntryDirection getDirection() {
        return direction;
    }

    public void setDirection(LedgerEntryDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public int getEntryOrder() {
        return entryOrder;
    }

    public void setEntryOrder(int entryOrder) {
        this.entryOrder = entryOrder;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
