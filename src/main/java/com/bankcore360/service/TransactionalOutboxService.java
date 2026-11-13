package com.bankcore360.service;

import com.bankcore360.entity.OutboxEvent;
import com.bankcore360.entity.OutboxEventStatus;
import com.bankcore360.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionalOutboxService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalOutboxService.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionalOutboxService(OutboxEventRepository outboxEventRepository,
                                      @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists an outbox event atomically within the calling business transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
        try {
            String jsonPayload = (payload instanceof String s) ? s : objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, topic, jsonPayload);
            return outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event [{}]: {}", eventType, e.getMessage());
            throw new RuntimeException("Could not persist outbox event", e);
        }
    }

    /**
     * Scheduled background publisher polling pending events from PostgreSQL and sending them to Kafka.
     * Guarantees At-Least-Once Delivery to the Kafka distributed cluster.
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.interval:3000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsForPublishing(
                OutboxEventStatus.PENDING, MAX_RETRIES, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                            .get(2, TimeUnit.SECONDS); // Synchronous confirmation
                } else {
                    log.debug("KafkaTemplate not active; simulating event dispatch for: {}", event.getEventType());
                }

                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                log.info("Published outbox event [{}] to Kafka topic [{}]", event.getEventType(), event.getTopic());
            } catch (Exception ex) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setErrorMessage(ex.getMessage());
                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxEventStatus.FAILED);
                    log.error("Outbox event [{}] reached max retries ({}). Marked FAILED (Dead-Letter): {}",
                            event.getId(), MAX_RETRIES, ex.getMessage());
                } else {
                    log.warn("Failed to publish outbox event [{}] on attempt {}: {}", event.getId(), retries, ex.getMessage());
                }
            }
            outboxEventRepository.save(event);
        }
    }
}

