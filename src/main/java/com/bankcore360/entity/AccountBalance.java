package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "account_balances",
    indexes = {
        @Index(name = "idx_acc_balance_account", columnList = "account_id", unique = true)
    }
)
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private LedgerAccount account;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "pending_debit", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingDebit = BigDecimal.ZERO;

    @Column(name = "pending_credit", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingCredit = BigDecimal.ZERO;

    @Column(name = "last_transaction_id")
    private Long lastTransactionId;

    @Column(name = "last_transaction_reference", length = 64)
    private String lastTransactionReference;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AccountBalance() {
    }

    public AccountBalance(LedgerAccount account) {
        this.account = account;
        this.currentBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.pendingDebit = BigDecimal.ZERO;
        this.pendingCredit = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public AccountBalance(LedgerAccount account, BigDecimal initialBalance) {
        this.account = account;
        this.currentBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.availableBalance = this.currentBalance;
        this.pendingDebit = BigDecimal.ZERO;
        this.pendingCredit = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LedgerAccount getAccount() {
        return account;
    }

    public void setAccount(LedgerAccount account) {
        this.account = account;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getPendingDebit() {
        return pendingDebit;
    }

    public void setPendingDebit(BigDecimal pendingDebit) {
        this.pendingDebit = pendingDebit;
    }

    public BigDecimal getPendingCredit() {
        return pendingCredit;
    }

    public void setPendingCredit(BigDecimal pendingCredit) {
        this.pendingCredit = pendingCredit;
    }

    public Long getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(Long lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
    }

    public String getLastTransactionReference() {
        return lastTransactionReference;
    }

    public void setLastTransactionReference(String lastTransactionReference) {
        this.lastTransactionReference = lastTransactionReference;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
