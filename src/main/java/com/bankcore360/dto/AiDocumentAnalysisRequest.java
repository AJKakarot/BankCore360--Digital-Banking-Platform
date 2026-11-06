package com.bankcore360.dto;

import com.bankcore360.entity.LoanDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AiDocumentAnalysisRequest {

    @NotNull(message = "Document type is required")
    private LoanDocumentType documentType;

    @NotBlank(message = "Document metadata or text sample is required")
    private String simulatedDocumentText;

    private String applicantName;

    public AiDocumentAnalysisRequest() {
    }

    public AiDocumentAnalysisRequest(LoanDocumentType documentType, String simulatedDocumentText, String applicantName) {
        this.documentType = documentType;
        this.simulatedDocumentText = simulatedDocumentText;
        this.applicantName = applicantName;
    }

    public LoanDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LoanDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getSimulatedDocumentText() {
        return simulatedDocumentText;
    }

    public void setSimulatedDocumentText(String simulatedDocumentText) {
        this.simulatedDocumentText = simulatedDocumentText;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }
}

