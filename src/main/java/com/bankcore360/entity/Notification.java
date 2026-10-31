package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an in-app notification delivered to a user (Customer, Checker, Manager, Admin).
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipientUsername"),
        @Index(name = "idx_notification_read", columnList = "readStatus")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String recipientUsername;

    @Column(length = 64)
    private String notificationType = "SYSTEM";

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private boolean readStatus = false;

    @Column(length = 64)
    private String relatedReference;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    public Notification(String recipientUsername, String title, String message, String relatedReference) {
        this(recipientUsername, "SYSTEM", title, message, relatedReference);
    }

    public Notification(String recipientUsername, String notificationType, String title, String message, String relatedReference) {
        this.recipientUsername = recipientUsername;
        this.notificationType = notificationType != null ? notificationType : "SYSTEM";
        this.title = title;
        this.message = message;
        this.relatedReference = relatedReference;
        this.readStatus = false;
        this.createdAt = LocalDateTime.now();
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

