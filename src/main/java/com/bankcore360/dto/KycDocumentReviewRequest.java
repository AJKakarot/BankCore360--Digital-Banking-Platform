package com.bankcore360.dto;

import com.bankcore360.entity.DocumentVerificationStatus;
import jakarta.validation.constraints.NotNull;

public class KycDocumentReviewRequest {

    @NotNull(message = "status is required")
    private DocumentVerificationStatus status;

    private String rejectionReason;

    public KycDocumentReviewRequest() {
    }

    public KycDocumentReviewRequest(DocumentVerificationStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public DocumentVerificationStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentVerificationStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

