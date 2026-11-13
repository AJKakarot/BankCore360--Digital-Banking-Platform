package com.bankcore360.dto;

import com.bankcore360.entity.FraudAlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateAlertStatusRequest {

    @NotNull(message = "Target status is required")
    private FraudAlertStatus status;

    @NotBlank(message = "Investigation notes are mandatory for any status change")
    private String notes;

    private String actionTaken;

    public UpdateAlertStatusRequest() {
    }

    public UpdateAlertStatusRequest(FraudAlertStatus status, String notes, String actionTaken) {
        this.status = status;
        this.notes = notes;
        this.actionTaken = actionTaken;
    }

    public FraudAlertStatus getStatus() {
        return status;
    }

    public void setStatus(FraudAlertStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }
}

