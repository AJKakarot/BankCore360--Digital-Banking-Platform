package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ledger_accounts",
    indexes = {
        @Index(name = "idx_ledger_acc_number", columnList = "accountNumber", unique = true),
        @Index(name = "idx_ledger_acc_type", columnList = "accountType")
    }
)
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String accountNumber;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private LedgerAccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerAccountStatus status = LedgerAccountStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "is_system_account", nullable = false)
    private boolean isSystemAccount = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public LedgerAccount() {
    }

    public LedgerAccount(String accountNumber, String name, LedgerAccountType accountType,
                         String currency, LedgerAccountStatus status, Customer customer, boolean isSystemAccount) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.accountType = accountType;
        this.currency = (currency != null && !currency.isBlank()) ? currency.toUpperCase() : "INR";
        this.status = status != null ? status : LedgerAccountStatus.ACTIVE;
        this.customer = customer;
        this.isSystemAccount = isSystemAccount;
        this.createdAt = LocalDateTime.now();
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(LedgerAccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LedgerAccountStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public boolean isSystemAccount() {
        return isSystemAccount;
    }

    public void setSystemAccount(boolean systemAccount) {
        isSystemAccount = systemAccount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
