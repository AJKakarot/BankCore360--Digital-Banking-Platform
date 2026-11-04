package com.bankcore360.kafka.consumer;

import com.bankcore360.entity.Customer;
import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanDocument;
import com.bankcore360.entity.LoanStatus;
import com.bankcore360.event.EventEnvelope;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.repository.CustomerRepository;
import com.bankcore360.repository.LoanApplicationRepository;
import com.bankcore360.repository.LoanDocumentRepository;
import com.bankcore360.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consumer for KYC document verification & rejection events on the kyc-events topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KycEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KycEventConsumer.class);
    public static final String CONSUMER_GROUP = "bankcore360-kyc-group";

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final ObjectMapper objectMapper;

    public KycEventConsumer(
            ProcessedEventRepository processedEventRepository,
            NotificationService notificationService,
            CustomerRepository customerRepository,
            LoanApplicationRepository loanApplicationRepository,
            LoanDocumentRepository loanDocumentRepository,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanDocumentRepository = loanDocumentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.kyc-events:kyc-events}",
            groupId = "${app.kafka.consumer-groups.kyc:bankcore360-kyc-group}",
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
            log.info("Received event [eventId={}, type={}, aggregateId={}] on kyc-events",
                    eventId, envelope.eventType(), envelope.aggregateId());

            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate event [eventId={}] for consumer group [{}]", eventId, CONSUMER_GROUP);
                return;
            }

            Map<String, Object> payload = envelope.payload();
            String appRef = (String) payload.get("applicationReference");
            Number custIdNum = (Number) payload.get("customerId");
            Long customerId = custIdNum != null ? custIdNum.longValue() : null;
            String docType = (String) payload.get("documentType");

            String recipientEmail = null;
            if (customerId != null) {
                Optional<Customer> custOpt = customerRepository.findById(customerId);
                if (custOpt.isPresent()) {
                    recipientEmail = custOpt.get().getEmail();
                }
            }

            if ("KYC_DOCUMENT_VERIFIED".equals(envelope.eventType())) {
                String verifiedBy = (String) payload.get("verifiedBy");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "DOC_VERIFIED",
                            "Document Verified",
                            "Your " + docType + " document for loan " + appRef + " was successfully verified by " + verifiedBy + ".",
                            appRef
                    );
                }

                // Workflow check: If all documents for this loan are now verified, check readiness
                if (appRef != null) {
                    Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationReference(appRef);
                    if (appOpt.isPresent()) {
                        List<LoanDocument> docs = loanDocumentRepository.findByLoanApplicationIdOrderByUploadedAtDesc(appOpt.get().getId());
                        boolean allVerified = !docs.isEmpty() && docs.stream().allMatch(d -> d.getDocumentStatus() == DocumentVerificationStatus.VERIFIED);
                        if (allVerified && appOpt.get().getStatus() == LoanStatus.UNDER_REVIEW) {
                            log.info("All documents verified for loan application [{}]. Ready for Manager forwarding.", appRef);
                        }
                    }
                }
            } else if ("KYC_DOCUMENT_REJECTED".equals(envelope.eventType())) {
                String reason = (String) payload.get("rejectionReason");
                if (recipientEmail != null) {
                    notificationService.notifyUser(
                            recipientEmail,
                            "DOC_REJECTED",
                            "Document Rejected",
                            "Your " + docType + " document for loan " + appRef + " was rejected. Reason: " + reason,
                            appRef
                    );
                }
            }

            processedEventRepository.save(new ProcessedEvent(eventId, envelope.eventType(), CONSUMER_GROUP));
        } catch (Exception ex) {
            log.error("Failed to process KYC document event record: {}", recordValue, ex);
            throw new RuntimeException("Error processing KYC event: " + ex.getMessage(), ex);
        }
    }
}

