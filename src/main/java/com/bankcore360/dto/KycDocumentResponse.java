package com.bankcore360.dto;

import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.KycDocumentType;
import java.time.LocalDateTime;

public class KycDocumentResponse {

    private Long id;
    private Long customerId;
    private KycDocumentType documentType;
    private String documentUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private DocumentVerificationStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;

    public KycDocumentResponse() {
    }

    public KycDocumentResponse(Long id, Long customerId, KycDocumentType documentType, String documentUrl,
                               String fileName, Long fileSize, String contentType,
                               DocumentVerificationStatus verificationStatus, String rejectionReason,
                               LocalDateTime uploadedAt, LocalDateTime reviewedAt, String reviewedBy) {
        this.id = id;
        this.customerId = customerId;
        this.documentType = documentType;
        this.documentUrl = documentUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.verificationStatus = verificationStatus;
        this.rejectionReason = rejectionReason;
        this.uploadedAt = uploadedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
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

    public KycDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(KycDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
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

    public DocumentVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(DocumentVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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

