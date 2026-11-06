package com.bankcore360.dto;

import java.time.LocalDateTime;

public class KycAuditLogResponse {

    private Long id;
    private Long customerId;
    private String previousStatus;
    private String newStatus;
    private String reviewerUsername;
    private LocalDateTime createdAt;

    public KycAuditLogResponse() {
    }

    public KycAuditLogResponse(Long customerId, String previousStatus,
                              String newStatus, String reviewerUsername, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerUsername = reviewerUsername;
        this.createdAt = createdAt;
    }

    public KycAuditLogResponse(Long customerId, com.bankcore360.entity.KycStatus previousStatus,
                              com.bankcore360.entity.KycStatus newStatus, String reviewerUsername, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.previousStatus = previousStatus != null ? previousStatus.name() : null;
        this.newStatus = newStatus != null ? newStatus.name() : null;
        this.reviewerUsername = reviewerUsername;
        this.createdAt = createdAt;
    }

    public KycAuditLogResponse(Long id, Long customerId, String previousStatus,
                              String newStatus, String reviewerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerUsername = reviewerUsername;
        this.createdAt = createdAt;
    }

    public KycAuditLogResponse(Long id, Long customerId, com.bankcore360.entity.KycStatus previousStatus,
                              com.bankcore360.entity.KycStatus newStatus, String reviewerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.previousStatus = previousStatus != null ? previousStatus.name() : null;
        this.newStatus = newStatus != null ? newStatus.name() : null;
        this.reviewerUsername = reviewerUsername;
        this.createdAt = createdAt;
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

    // Compatibility accessors
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
