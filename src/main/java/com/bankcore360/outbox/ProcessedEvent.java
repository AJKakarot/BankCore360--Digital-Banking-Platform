package com.bankcore360.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Idempotency tracking entity for event consumer workers.
 * Prevents duplicate processing of re-delivered events.
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_event_group", columnList = "eventId, consumerGroup", unique = true)
})
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String eventType, String consumerGroup) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.consumerGroup = consumerGroup;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}

