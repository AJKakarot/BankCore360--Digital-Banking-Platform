package com.bankcore360.dto;

import java.time.LocalDateTime;

public class LoanAuditLogResponse {

    private Long id;
    private String applicationReference;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String previousStatus;
    private String newStatus;
    private String remarks;
    private LocalDateTime timestamp;

    public LoanAuditLogResponse() {
    }

    public LoanAuditLogResponse(Long id, String applicationReference, String actorUsername,
                                String actorRole, String action, String previousStatus,
                                String newStatus, String remarks, LocalDateTime timestamp) {
        this.id = id;
        this.applicationReference = applicationReference;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

