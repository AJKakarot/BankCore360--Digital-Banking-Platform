package com.bankcore360.outbox;

import com.bankcore360.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementing the Transactional Outbox Pattern.
 * Guarantees that business state changes in PostgreSQL and event generation succeed or fail together.
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private static final int MAX_OUTBOX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean kafkaEnabled;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.enabled:true}") boolean kafkaEnabled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaEnabled = kafkaEnabled;
    }

    /**
     * Persists an event in the outbox table within the current database transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> OutboxEvent saveEvent(String topic, EventEnvelope<T> envelope) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            OutboxEvent outboxEvent = new OutboxEvent(
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.aggregateId(),
                    topic,
                    jsonPayload
            );
            return outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize event envelope [eventId={}] to JSON", envelope.eventId(), ex);
            throw new IllegalArgumentException("Cannot serialize event to JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Background worker that periodically polls for PENDING outbox events and dispatches them to Kafka.
     */
    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        if (!kafkaEnabled) {
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    @Transactional
    public void publishSingleEvent(OutboxEvent event) {
        try {
            Object eventPayload = objectMapper.readValue(event.getPayloadJson(), Object.class);

            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), eventPayload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markEventAsPublished(event.getId());
                            log.info("Successfully published Outbox event [id={}, eventId={}, topic={}, offset={}]",
                                    event.getId(), event.getEventId(), event.getTopic(),
                                    result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : "N/A");
                        } else {
                            handlePublishFailure(event.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            handlePublishFailure(event.getId(), ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEventAsPublished(Long outboxId) {
        outboxEventRepository.findById(outboxId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePublishFailure(Long outboxId, String errorMessage) {
        outboxEventRepository.findById(outboxId).ifPresent(event -> {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(errorMessage);
            if (event.getRetryCount() >= MAX_OUTBOX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event [id={}, eventId={}] exceeded max retries. Marked as FAILED.",
                        event.getId(), event.getEventId());
            } else {
                log.warn("Failed to publish Outbox event [id={}, eventId={}]. Attempt {}/{}",
                        event.getId(), event.getEventId(), event.getRetryCount(), MAX_OUTBOX_RETRIES);
            }
            outboxEventRepository.save(event);
        });
    }
}

