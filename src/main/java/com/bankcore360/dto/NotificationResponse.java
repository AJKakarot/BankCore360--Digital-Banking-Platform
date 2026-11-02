package com.bankcore360.dto;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private String recipientUsername;
    private String notificationType;
    private String title;
    private String message;
    private boolean readStatus;
    private String relatedReference;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, String recipientUsername, String notificationType,
                                String title, String message, boolean readStatus,
                                String relatedReference, LocalDateTime createdAt) {
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.readStatus = readStatus;
        this.relatedReference = relatedReference;
        this.createdAt = createdAt;
    }

    public NotificationResponse(Long id, String recipientUsername, String title,
                                String message, boolean readStatus, String relatedReference,
                                LocalDateTime createdAt) {
        this(id, recipientUsername, "SYSTEM", title, message, readStatus, relatedReference, createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public void setRelatedReference(String relatedReference) {
        this.relatedReference = relatedReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

