package com.bankcore360.kafka.consumer;

import com.bankcore360.event.EventEnvelope;
import com.bankcore360.event.payload.LoanApplicationSubmittedPayload;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer for loan lifecycle events published to the loan-events topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class LoanEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-loan-group";

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public LoanEventConsumer(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.loan-events:loan-events}",
            groupId = "${app.kafka.consumer-groups.loan:bankcore360-loan-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(Object recordValue) {
        try {
            EventEnvelope<LoanApplicationSubmittedPayload> envelope = objectMapper.convertValue(
                    recordValue,
                    new TypeReference<>() {}
            );

            String eventId = envelope.eventId();
            log.info("Received event [eventId={}, type={}, aggregateId={}] on loan-events",
                    eventId, envelope.eventType(), envelope.aggregateId());

            // Idempotency check: Skip if already processed by this consumer group
            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            if ("LOAN_APPLICATION_SUBMITTED".equals(envelope.eventType())) {
                LoanApplicationSubmittedPayload payload = envelope.payload();
                String message = "New loan application " + payload.applicationReference()
                        + " (" + payload.loanType() + ", ₹" + payload.requestedAmount()
                        + ") requires document review.";

                // Notify Checker staff queue
                notificationService.notifyUser(
                        "dev_checker",
                        "LOAN_SUBMITTED",
                        "Pending Loan Review",
                        message,
                        payload.applicationReference()
                );
            }

            // Record as processed in the same database transaction
            processedEventRepository.save(new ProcessedEvent(eventId, envelope.eventType(), CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process loan event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing loan event: " + ex.getMessage(), ex);
        }
    }
}

