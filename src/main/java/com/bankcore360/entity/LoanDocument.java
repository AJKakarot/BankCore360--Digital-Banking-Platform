package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a supporting document attached to a Loan Application.
 */
@Entity
@Table(name = "loan_documents", indexes = {
        @Index(name = "idx_loan_doc_app", columnList = "loan_application_id"),
        @Index(name = "idx_loan_doc_status", columnList = "documentStatus")
})
public class LoanDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoanDocumentType documentType;

    @Column(nullable = false, length = 255)
    private String storageReference;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentVerificationStatus documentStatus = DocumentVerificationStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    public LoanDocument() {
    }

    public LoanDocument(LoanApplication loanApplication, LoanDocumentType documentType,
                        String storageReference, String fileName, Long fileSize, String contentType) {
        this.loanApplication = loanApplication;
        this.documentType = documentType;
        this.storageReference = storageReference;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.documentStatus = DocumentVerificationStatus.PENDING;
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LoanApplication getLoanApplication() {
        return loanApplication;
    }

    public void setLoanApplication(LoanApplication loanApplication) {
        this.loanApplication = loanApplication;
    }

    public LoanDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LoanDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public DocumentVerificationStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentVerificationStatus documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}

