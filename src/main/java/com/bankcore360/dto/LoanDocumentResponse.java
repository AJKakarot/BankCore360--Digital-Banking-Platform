package com.bankcore360.dto;

import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.LoanDocumentType;

import java.time.LocalDateTime;

public class LoanDocumentResponse {

    private Long id;
    private Long loanApplicationId;
    private String applicationReference;
    private LoanDocumentType documentType;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private DocumentVerificationStatus documentStatus;
    private String rejectionReason;
    private String remarks;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String downloadUrl;

    public LoanDocumentResponse() {
    }

    public LoanDocumentResponse(Long id, Long loanApplicationId, String applicationReference,
                                LoanDocumentType documentType, String fileName, Long fileSize,
                                String contentType, DocumentVerificationStatus documentStatus,
                                String rejectionReason, String remarks, LocalDateTime uploadedAt,
                                LocalDateTime reviewedAt, String reviewedBy, String downloadUrl) {
        this.id = id;
        this.loanApplicationId = loanApplicationId;
        this.applicationReference = applicationReference;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.documentStatus = documentStatus;
        this.rejectionReason = rejectionReason;
        this.remarks = remarks;
        this.uploadedAt = uploadedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.downloadUrl = downloadUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanApplicationId() {
        return loanApplicationId;
    }

    public void setLoanApplicationId(Long loanApplicationId) {
        this.loanApplicationId = loanApplicationId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public LoanDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LoanDocumentType documentType) {
        this.documentType = documentType;
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

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}

