package com.bankcore360.kafka.consumer;

import com.bankcore360.entity.Customer;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Consumer for manager credit decisions on the approval-events topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ApprovalEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ApprovalEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-approval-group";

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public ApprovalEventConsumer(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService,
            CustomerRepository customerRepository,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.approval-events:approval-events}",
            groupId = "${app.kafka.consumer-groups.approval:bankcore360-approval-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(Object recordValue) {
        try {
            EventEnvelope<Map<String, Object>> envelope = objectMapper.convertValue(
                    recordValue,
                    new TypeReference<>() {}
            );

            String eventId = envelope.eventId();
            log.info("Received event [eventId={}, type={}, aggregateId={}] on approval-events",
                    eventId, envelope.eventType(), envelope.aggregateId());

            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            Map<String, Object> payload = envelope.payload();
            String appRef = (String) payload.get("applicationReference");
            Number custIdNum = (Number) payload.get("customerId");
            Long customerId = custIdNum != null ? custIdNum.longValue() : null;

            String recipientEmail = null;
            if (customerId != null) {
                Optional<Customer> custOpt = customerRepository.findById(customerId);
                if (custOpt.isPresent()) {
                    recipientEmail = custOpt.get().getEmail();
                }
            }

            if ("LOAN_APPROVED".equals(envelope.eventType())) {
                Object amount = payload.get("approvedAmount");
                Object rate = payload.get("interestRate");
                Object tenure = payload.get("approvedTenureMonths");

                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "LOAN_APPROVED",
                            "Loan Application Approved",
                            "Your loan " + appRef + " has been approved for ₹" + amount + " at " + rate + "% p.a. for " + tenure + " months.",
                            appRef
                    );
                }
            } else if ("LOAN_REJECTED".equals(envelope.eventType())) {
                String reason = (String) payload.get("rejectionReason");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "LOAN_REJECTED",
                            "Loan Application Rejected",
                            "Your loan application " + appRef + " was rejected. Reason: " + reason,
                            appRef
                    );
                }
            }

            processedEventRepository.save(new ProcessedEvent(eventId, envelope.eventType(), CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process approval event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing approval event: " + ex.getMessage(), ex);
        }
    }
}

