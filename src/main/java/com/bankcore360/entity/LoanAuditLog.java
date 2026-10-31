package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit log record for tracking all state changes, decisions, and reviewer actions on loan applications.
 */
@Entity
@Table(name = "loan_audit_logs", indexes = {
        @Index(name = "idx_loan_audit_ref", columnList = "applicationReference"),
        @Index(name = "idx_loan_audit_action", columnList = "action"),
        @Index(name = "idx_loan_audit_actor", columnList = "actorUsername")
})
public class LoanAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String applicationReference;

    @Column(nullable = false, length = 64)
    private String actorUsername;

    @Column(nullable = false, length = 32)
    private String actorRole;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 64)
    private String previousStatus;

    @Column(length = 64)
    private String newStatus;

    @Column(length = 2000)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public LoanAuditLog() {
    }

    public LoanAuditLog(String applicationReference, String actorUsername, String actorRole,
                        String action, String previousStatus, String newStatus, String remarks) {
        this.applicationReference = applicationReference;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

