package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_audit_logs")
public class AccountAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus newStatus;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public AccountAuditLog() {
    }

    public AccountAuditLog(String accountNumber, Long customerId, AccountStatus previousStatus,
                           AccountStatus newStatus, String action, String performedBy) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.performedBy = performedBy;
        this.timestamp = LocalDateTime.now();
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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AccountStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AccountStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AccountStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AccountStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

