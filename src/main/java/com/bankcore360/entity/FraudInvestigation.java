package com.bankcore360.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fraud_investigations",
    indexes = {
        @Index(name = "idx_inv_alert_id", columnList = "alert_id"),
        @Index(name = "idx_inv_created_at", columnList = "createdAt")
    }
)
public class FraudInvestigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private FraudAlert alert;

    @Column(name = "investigator_username", nullable = false, length = 64)
    private String investigatorUsername;

    @Column(name = "action_taken", nullable = false, length = 50)
    private String actionTaken;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FraudInvestigation() {
    }

    public FraudInvestigation(FraudAlert alert, String investigatorUsername, String actionTaken, String notes) {
        this.alert = alert;
        this.investigatorUsername = investigatorUsername;
        this.actionTaken = actionTaken;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FraudAlert getAlert() {
        return alert;
    }

    public void setAlert(FraudAlert alert) {
        this.alert = alert;
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
}
