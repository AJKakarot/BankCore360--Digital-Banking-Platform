package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_audit_logs")
public class KycAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus newStatus;

    @Column(nullable = false)
    private String reviewerUsername;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public KycAuditLog() {
    }

    public KycAuditLog(Long customerId, KycStatus previousStatus, KycStatus newStatus, String reviewerUsername) {
        this(customerId, previousStatus, newStatus, reviewerUsername, LocalDateTime.now());
    }

    public KycAuditLog(Long customerId, KycStatus previousStatus, KycStatus newStatus, String reviewerUsername, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerUsername = reviewerUsername;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public KycStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(KycStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public KycStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(KycStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Compatibility accessors for existing audit mapping and tests
    public String getReviewedBy() {
        return reviewerUsername;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewerUsername = reviewedBy;
    }

    public LocalDateTime getTimestamp() {
        return createdAt;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.createdAt = timestamp;
    }
}
