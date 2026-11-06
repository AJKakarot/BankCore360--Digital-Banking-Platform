package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_document_audit_logs")
public class KycDocumentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVerificationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVerificationStatus newStatus;

    @Column(nullable = false)
    private String reviewerUsername;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public KycDocumentAuditLog() {
    }

    public KycDocumentAuditLog(Long documentId, Long customerId,
                               DocumentVerificationStatus previousStatus,
                               DocumentVerificationStatus newStatus,
                               String reviewerUsername,
                               String rejectionReason) {
        this.documentId = documentId;
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerUsername = reviewerUsername;
        this.rejectionReason = rejectionReason;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public DocumentVerificationStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(DocumentVerificationStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public DocumentVerificationStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(DocumentVerificationStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

