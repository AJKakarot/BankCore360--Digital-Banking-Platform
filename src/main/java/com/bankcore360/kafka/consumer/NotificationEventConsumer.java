package com.bankcore360.kafka.consumer;

import com.bankcore360.event.EventEnvelope;
import com.bankcore360.event.payload.NotificationEventPayload;
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
 * Consumer for general notification events published to the notification-events topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-notification-group";

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events:notification-events}",
            groupId = "${app.kafka.consumer-groups.notification:bankcore360-notification-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(Object recordValue) {
        try {
            EventEnvelope<NotificationEventPayload> envelope = objectMapper.convertValue(
                    recordValue,
                    new TypeReference<>() {}
            );

            String eventId = envelope.eventId();
            log.info("Received event [eventId={}, type={}, aggregateId={}] on notification-events",
                    eventId, envelope.eventType(), envelope.aggregateId());

            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            NotificationEventPayload payload = envelope.payload();
            notificationService.notifyUser(
                    payload.recipientUsername(),
                    payload.notificationType(),
                    payload.title(),
                    payload.message(),
                    payload.relatedReference()
            );

            processedEventRepository.save(new ProcessedEvent(eventId, envelope.eventType(), CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process notification event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing notification event: " + ex.getMessage(), ex);
        }
    }
}

