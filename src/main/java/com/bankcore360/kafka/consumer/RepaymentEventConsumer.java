package com.bankcore360.kafka.consumer;

import com.bankcore360.entity.LoanApplication;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.repository.LoanApplicationRepository;
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
 * Consumer for EMI due, payment, and overdue events on repayment-events topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class RepaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RepaymentEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-repayment-group";

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final ObjectMapper objectMapper;

    public RepaymentEventConsumer(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService,
            LoanApplicationRepository loanApplicationRepository,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.loanApplicationRepository = loanApplicationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.repayment-events:repayment-events}",
            groupId = "${app.kafka.consumer-groups.repayment:bankcore360-repayment-group}",
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
            log.info("Received event [eventId={}, type={}, aggregateId={}] on repayment-events",
                    eventId, envelope.eventType(), envelope.aggregateId());

            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            Map<String, Object> payload = envelope.payload();
            String appRef = (String) payload.get("applicationReference");
            Object installmentNum = payload.get("installmentNumber");
            Object emiAmt = payload.get("emiAmount");

            String recipientEmail = null;
            if (appRef != null) {
                Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationReference(appRef);
                if (appOpt.isPresent()) {
                    recipientEmail = appOpt.get().getCustomer().getEmail();
                }
            }

            if ("EMI_DUE".equals(envelope.eventType())) {
                Object dueDate = payload.get("dueDate");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "EMI_DUE",
                            "Upcoming EMI Due Reminder",
                            "EMI installment #" + installmentNum + " of ₹" + emiAmt + " for loan " + appRef + " is due on " + dueDate + ".",
                            appRef
                    );
                }
            } else if ("EMI_PAID".equals(envelope.eventType())) {
                Object paymentRef = payload.get("paymentReference");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "EMI_PAID",
                            "EMI Payment Received",
                            "Payment of ₹" + emiAmt + " for installment #" + installmentNum + " (Loan: " + appRef + ") was recorded. Ref: " + paymentRef,
                            appRef
                    );
                }
            } else if ("EMI_OVERDUE".equals(envelope.eventType())) {
                Object daysOverdue = payload.get("daysOverdue");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "EMI_OVERDUE",
                            "URGENT: EMI Overdue",
                            "Your EMI installment #" + installmentNum + " of ₹" + emiAmt + " for loan " + appRef + " is overdue by " + daysOverdue + " days.",
                            appRef
                    );
                }
            }

            processedEventRepository.save(new ProcessedEvent(eventId, envelope.eventType(), CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process repayment event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing repayment event: " + ex.getMessage(), ex);
        }
    }
}

