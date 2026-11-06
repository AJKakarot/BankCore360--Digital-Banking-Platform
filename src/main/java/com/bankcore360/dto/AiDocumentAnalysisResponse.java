package com.bankcore360.dto;

import com.bankcore360.entity.LoanDocumentType;

import java.util.List;
import java.util.Map;

public class AiDocumentAnalysisResponse {

    private LoanDocumentType documentType;
    private boolean metadataComplete;
    private Map<String, String> extractedFields;
    private List<String> flaggedInconsistencies;
    private List<String> humanReviewerChecklist;
    private String advisoryNotice = "AI Document Pre-Check for advisory assistance only. Mandatory physical/digital verification must be finalized by human Checker.";

    public AiDocumentAnalysisResponse() {
    }

    public AiDocumentAnalysisResponse(LoanDocumentType documentType, boolean metadataComplete,
                                      Map<String, String> extractedFields,
                                      List<String> flaggedInconsistencies,
                                      List<String> humanReviewerChecklist) {
        this.documentType = documentType;
        this.metadataComplete = metadataComplete;
        this.extractedFields = extractedFields;
        this.flaggedInconsistencies = flaggedInconsistencies;
        this.humanReviewerChecklist = humanReviewerChecklist;
    }

    public LoanDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LoanDocumentType documentType) {
        this.documentType = documentType;
    }

    public boolean isMetadataComplete() {
        return metadataComplete;
    }

    public void setMetadataComplete(boolean metadataComplete) {
        this.metadataComplete = metadataComplete;
    }

    public Map<String, String> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(Map<String, String> extractedFields) {
        this.extractedFields = extractedFields;
    }

    public List<String> getFlaggedInconsistencies() {
        return flaggedInconsistencies;
    }

    public void setFlaggedInconsistencies(List<String> flaggedInconsistencies) {
        this.flaggedInconsistencies = flaggedInconsistencies;
    }

    public List<String> getHumanReviewerChecklist() {
        return humanReviewerChecklist;
    }

    public void setHumanReviewerChecklist(List<String> humanReviewerChecklist) {
        this.humanReviewerChecklist = humanReviewerChecklist;
    }

    public String getAdvisoryNotice() {
        return advisoryNotice;
    }

    public void setAdvisoryNotice(String advisoryNotice) {
        this.advisoryNotice = advisoryNotice;
    }
}

