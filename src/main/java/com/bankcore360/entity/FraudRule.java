package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fraud_rules",
    indexes = {
        @Index(name = "idx_fraud_rule_code", columnList = "ruleCode", unique = true),
        @Index(name = "idx_fraud_rule_active", columnList = "isActive")
    }
)
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private FraudRuleType ruleType;

    @Column(name = "weight_score", nullable = false)
    private int weightScore;

    @Column(name = "threshold_value", precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "time_window_seconds")
    private Integer timeWindowSeconds;

    @Column(name = "max_allowed_events")
    private Integer maxAllowedEvents;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public FraudRule() {
    }

    public FraudRule(String ruleCode, String name, String description, FraudRuleType ruleType,
                     int weightScore, BigDecimal thresholdValue, Integer timeWindowSeconds,
                     Integer maxAllowedEvents, boolean isActive) {
        this.ruleCode = ruleCode;
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.weightScore = weightScore;
        this.thresholdValue = thresholdValue;
        this.timeWindowSeconds = timeWindowSeconds;
        this.maxAllowedEvents = maxAllowedEvents;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FraudRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(FraudRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public int getWeightScore() {
        return weightScore;
    }

    public void setWeightScore(int weightScore) {
        this.weightScore = weightScore;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Integer getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public void setTimeWindowSeconds(Integer timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }

    public Integer getMaxAllowedEvents() {
        return maxAllowedEvents;
    }

    public void setMaxAllowedEvents(Integer maxAllowedEvents) {
        this.maxAllowedEvents = maxAllowedEvents;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
