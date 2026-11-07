package com.bankcore360.outbox;

import com.bankcore360.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service implementing the Transactional Outbox Pattern for enterprise event audit history.
 * Guarantees that business state changes in PostgreSQL and event generation succeed or fail together.
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
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
            outboxEvent.setStatus(OutboxStatus.PUBLISHED);
            outboxEvent.setPublishedAt(LocalDateTime.now());
            return outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize event envelope [eventId={}] to JSON", envelope.eventId(), ex);
            throw new IllegalArgumentException("Cannot serialize event to JSON: " + ex.getMessage(), ex);
        }
    }
}

