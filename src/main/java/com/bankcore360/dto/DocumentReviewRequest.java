package com.bankcore360.dto;

import jakarta.validation.constraints.Size;

public class DocumentReviewRequest {

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;

    @Size(max = 1000, message = "Rejection reason cannot exceed 1000 characters")
    private String rejectionReason;

    public DocumentReviewRequest() {
    }

    public DocumentReviewRequest(String remarks, String rejectionReason) {
        this.remarks = remarks;
        this.rejectionReason = rejectionReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

