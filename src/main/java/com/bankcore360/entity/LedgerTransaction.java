package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "ledger_transactions",
    indexes = {
        @Index(name = "idx_ltx_reference", columnList = "transactionReference", unique = true),
        @Index(name = "idx_ltx_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_ltx_posted_at", columnList = "postedAt")
    }
)
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private LedgerTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerTransactionStatus status = LedgerTransactionStatus.POSTED;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 255)
    private String description;

    @Column(name = "initiated_by", length = 64)
    private String initiatedBy;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @OrderBy("entryOrder ASC")
    private List<LedgerEntry> entries = new ArrayList<>();

    public LedgerTransaction() {
    }

    public LedgerTransaction(String transactionReference, String idempotencyKey,
                             LedgerTransactionType transactionType, BigDecimal amount,
                             String currency, String description, String initiatedBy) {
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.status = LedgerTransactionStatus.POSTED;
        this.amount = amount;
        this.currency = (currency != null && !currency.isBlank()) ? currency.toUpperCase() : "INR";
        this.description = description;
        this.initiatedBy = initiatedBy;
        this.postedAt = LocalDateTime.now();
    }

    public void addEntry(LedgerEntry entry) {
        if (entry != null) {
            entry.setTransaction(this);
            entry.setEntryOrder(this.entries.size() + 1);
            this.entries.add(entry);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LedgerTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(LedgerTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public LedgerTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerTransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
    }
}
