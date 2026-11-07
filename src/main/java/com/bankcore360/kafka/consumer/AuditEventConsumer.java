package com.bankcore360.kafka.consumer;

import com.bankcore360.event.EventEnvelope;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.service.LoanAuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Dedicated Kafka Consumer for persisting audit log entries into PostgreSQL.
 * Listens to loan, document, and approval topics to record immutable audit history.
 * Ensures idempotent processing using event IDs.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-audit-group";

    private final ProcessedEventRepository processedEventRepository;
    private final LoanAuditService loanAuditService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(
            ProcessedEventRepository processedEventRepository,
            LoanAuditService loanAuditService,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.loanAuditService = loanAuditService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.loan-events:loan-events}",
                    "${app.kafka.topics.kyc-events:kyc-events}",
                    "${app.kafka.topics.approval-events:approval-events}"
            },
            groupId = "${app.kafka.consumer-groups.audit:bankcore360-audit-group}",
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
            String eventType = envelope.eventType();
            String aggregateId = envelope.aggregateId();

            log.info("Audit Consumer received event [eventId={}, type={}, aggregateId={}]",
                    eventId, eventType, aggregateId);

            // Idempotency check: Skip if already processed by this consumer group
            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate audit event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            Map<String, Object> payload = envelope.payload() != null ? envelope.payload() : Map.of();

            switch (eventType) {
                case "LOAN_APPLICATION_SUBMITTED" -> {
                    Object amount = payload.get("requestedAmount");
                    Object loanType = payload.get("loanType");
                    loanAuditService.logAction(
                            aggregateId,
                            "CUSTOMER",
                            "ROLE_CUSTOMER",
                            "AUDIT_LOAN_SUBMITTED",
                            null,
                            "SUBMITTED",
                            "Kafka audit record: Application " + aggregateId + " submitted (" + loanType + ", ₹" + amount + ")"
                    );
                }
                case "KYC_DOCUMENT_VERIFIED" -> {
                    String reviewer = (String) payload.getOrDefault("verifiedBy", "CHECKER");
                    Object docType = payload.get("documentType");
                    loanAuditService.logAction(
                            aggregateId,
                            reviewer,
                            "ROLE_CHECKER",
                            "AUDIT_DOCUMENT_VERIFIED",
                            "PENDING",
                            "VERIFIED",
                            "Kafka audit record: Document " + docType + " verified by " + reviewer
                    );
                }
                case "KYC_DOCUMENT_REJECTED" -> {
                    String reviewer = (String) payload.getOrDefault("rejectedBy", "CHECKER");
                    Object docType = payload.get("documentType");
                    String reason = (String) payload.getOrDefault("rejectionReason", "Not specified");
                    loanAuditService.logAction(
                            aggregateId,
                            reviewer,
                            "ROLE_CHECKER",
                            "AUDIT_DOCUMENT_REJECTED",
                            "PENDING",
                            "REJECTED",
                            "Kafka audit record: Document " + docType + " rejected. Reason: " + reason
                    );
                }
                case "LOAN_APPROVED" -> {
                    String manager = (String) payload.getOrDefault("reviewedBy", "MANAGER");
                    Object amount = payload.get("approvedAmount");
                    Object rate = payload.get("interestRate");
                    loanAuditService.logAction(
                            aggregateId,
                            manager,
                            "ROLE_MANAGER",
                            "AUDIT_LOAN_APPROVED",
                            "PENDING_MANAGER_APPROVAL",
                            "APPROVED",
                            "Kafka audit record: Loan approved for ₹" + amount + " at " + rate + "% p.a."
                    );
                }
                case "LOAN_REJECTED" -> {
                    String manager = (String) payload.getOrDefault("rejectedBy", "MANAGER");
                    String reason = (String) payload.getOrDefault("rejectionReason", "Not specified");
                    loanAuditService.logAction(
                            aggregateId,
                            manager,
                            "ROLE_MANAGER",
                            "AUDIT_LOAN_REJECTED",
                            "PENDING_MANAGER_APPROVAL",
                            "REJECTED",
                            "Kafka audit record: Loan rejected by manager. Reason: " + reason
                    );
                }
                default -> log.debug("Unhandled audit event type [{}] for aggregate [{}]", eventType, aggregateId);
            }

            // Save processed event for idempotency
            processedEventRepository.save(new ProcessedEvent(eventId, eventType, CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process audit event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing audit event: " + ex.getMessage(), ex);
        }
    }
}

