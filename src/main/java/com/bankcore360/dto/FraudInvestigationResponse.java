package com.bankcore360.dto;

import com.bankcore360.entity.FraudInvestigation;

import java.time.LocalDateTime;

public class FraudInvestigationResponse {

    private Long id;
    private Long alertId;
    private String investigatorUsername;
    private String actionTaken;
    private String notes;
    private LocalDateTime createdAt;

    public FraudInvestigationResponse() {
    }

    public FraudInvestigationResponse(FraudInvestigation investigation) {
        this.id = investigation.getId();
        this.alertId = investigation.getAlert() != null ? investigation.getAlert().getId() : null;
        this.investigatorUsername = investigation.getInvestigatorUsername();
        this.actionTaken = investigation.getActionTaken();
        this.notes = investigation.getNotes();
        this.createdAt = investigation.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getInvestigatorUsername() {
        return investigatorUsername;
    }

    public void setInvestigatorUsername(String investigatorUsername) {
        this.investigatorUsername = investigatorUsername;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

