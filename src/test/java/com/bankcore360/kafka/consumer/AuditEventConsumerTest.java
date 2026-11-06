package com.bankcore360.kafka.consumer;

import com.bankcore360.event.EventEnvelope;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.service.LoanAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private LoanAuditService loanAuditService;

    private ObjectMapper objectMapper;
    private AuditEventConsumer auditEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        auditEventConsumer = new AuditEventConsumer(processedEventRepository, loanAuditService, objectMapper);
    }

    @Test
    @DisplayName("Should process LOAN_APPLICATION_SUBMITTED audit event and save audit log")
    void shouldProcessLoanApplicationSubmittedEvent() {
        String eventId = "evt-loan-001";
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.create(
                "LOAN_APPLICATION_SUBMITTED",
                "LN-2026-TEST01",
                Map.of("loanType", "PERSONAL", "requestedAmount", 200000)
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), eq(AuditEventConsumer.CONSUMER_GROUP)))
                .thenReturn(false);

        auditEventConsumer.consume(envelope);

        verify(loanAuditService).logAction(
                eq("LN-2026-TEST01"),
                eq("CUSTOMER"),
                eq("ROLE_CUSTOMER"),
                eq("AUDIT_LOAN_SUBMITTED"),
                eq(null),
                eq("SUBMITTED"),
                contains("LN-2026-TEST01")
        );
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should process KYC_DOCUMENT_VERIFIED audit event")
    void shouldProcessDocumentVerifiedEvent() {
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.create(
                "KYC_DOCUMENT_VERIFIED",
                "LN-2026-TEST01",
                Map.of("documentType", "PAN", "verifiedBy", "dev_checker")
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), eq(AuditEventConsumer.CONSUMER_GROUP)))
                .thenReturn(false);

        auditEventConsumer.consume(envelope);

        verify(loanAuditService).logAction(
                eq("LN-2026-TEST01"),
                eq("dev_checker"),
                eq("ROLE_CHECKER"),
                eq("AUDIT_DOCUMENT_VERIFIED"),
                eq("PENDING"),
                eq("VERIFIED"),
                contains("PAN")
        );
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should process LOAN_APPROVED audit event")
    void shouldProcessLoanApprovedEvent() {
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.create(
                "LOAN_APPROVED",
                "LN-2026-TEST01",
                Map.of("approvedAmount", 250000, "interestRate", 10.5, "reviewedBy", "dev_manager")
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), eq(AuditEventConsumer.CONSUMER_GROUP)))
                .thenReturn(false);

        auditEventConsumer.consume(envelope);

        verify(loanAuditService).logAction(
                eq("LN-2026-TEST01"),
                eq("dev_manager"),
                eq("ROLE_MANAGER"),
                eq("AUDIT_LOAN_APPROVED"),
                eq("PENDING_MANAGER_APPROVAL"),
                eq("APPROVED"),
                contains("250000")
        );
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should skip duplicate event based on eventId idempotency check")
    void shouldSkipDuplicateEvent() {
        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.create(
                "LOAN_APPROVED",
                "LN-2026-TEST01",
                Map.of("approvedAmount", 250000)
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(any(), eq(AuditEventConsumer.CONSUMER_GROUP)))
                .thenReturn(true); // Already processed!

        auditEventConsumer.consume(envelope);

        verify(loanAuditService, never()).logAction(any(), any(), any(), any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }
}
