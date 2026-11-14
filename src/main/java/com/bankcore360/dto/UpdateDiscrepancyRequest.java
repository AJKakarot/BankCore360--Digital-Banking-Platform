package com.bankcore360.dto;

import com.bankcore360.entity.DiscrepancyStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateDiscrepancyRequest {

    @NotNull(message = "Target status is required")
    private DiscrepancyStatus status;

    private String notes;

    public UpdateDiscrepancyRequest() {
    }

    public UpdateDiscrepancyRequest(DiscrepancyStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public DiscrepancyStatus getStatus() {
        return status;
    }

    public void setStatus(DiscrepancyStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

