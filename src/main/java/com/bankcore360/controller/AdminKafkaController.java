package com.bankcore360.controller;

import com.bankcore360.dto.KafkaStreamStatusResponse;
import com.bankcore360.dto.KafkaStreamStatusResponse.TopicDetail;
import com.bankcore360.outbox.OutboxEvent;
import com.bankcore360.outbox.OutboxEventRepository;
import com.bankcore360.outbox.OutboxStatus;
import com.bankcore360.outbox.ProcessedEvent;
import com.bankcore360.outbox.ProcessedEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/kafka")
@Tag(name = "Admin Kafka & Outbox Monitor", description = "Endpoints for inspecting Kafka cluster status, transactional outbox events, and consumer idempotency")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKafkaController {

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanTopic;

    @Value("${app.kafka.topics.kyc-events:kyc-events}")
    private String kycTopic;

    @Value("${app.kafka.topics.approval-events:approval-events}")
    private String approvalTopic;

    @Value("${app.kafka.topics.repayment-events:repayment-events}")
    private String repaymentTopic;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationTopic;

    @Value("${app.kafka.consumer-groups.loan:bankcore360-loan-group}")
    private String loanGroup;

    @Value("${app.kafka.consumer-groups.kyc:bankcore360-kyc-group}")
    private String kycGroup;

    @Value("${app.kafka.consumer-groups.approval:bankcore360-approval-group}")
    private String approvalGroup;

    @Value("${app.kafka.consumer-groups.repayment:bankcore360-repayment-group}")
    private String repaymentGroup;

    @Value("${app.kafka.consumer-groups.notification:bankcore360-notification-group}")
    private String notificationGroup;

    @Value("${app.kafka.consumer-groups.audit:bankcore360-audit-group}")
    private String auditGroup;

    public AdminKafkaController(
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @GetMapping("/events")
    @Operation(summary = "Get Kafka topics, Outbox event stream, and idempotency status",
            description = "Returns real-time Kafka configuration, transactional outbox records, and processed consumer records")
    public ResponseEntity<KafkaStreamStatusResponse> getKafkaStreamStatus(
            @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.min(100, Math.max(10, limit));

        List<TopicDetail> topics = List.of(
                new TopicDetail(loanTopic, loanGroup + ", " + auditGroup, "Loan application creation, updates, and workflow changes"),
                new TopicDetail(kycTopic, kycGroup + ", " + auditGroup, "Customer KYC document uploads, review, verify, and reject events"),
                new TopicDetail(approvalTopic, approvalGroup + ", " + auditGroup, "Credit manager loan approval, rejection, and sanction decisions"),
                new TopicDetail(repaymentTopic, repaymentGroup, "EMI repayment schedules, payments, and installment notices"),
                new TopicDetail(notificationTopic, notificationGroup, "Real-time user notification alerts and communications")
        );

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalEvents", outboxEventRepository.count());
        stats.put("pendingEvents", outboxEventRepository.countByStatus(OutboxStatus.PENDING));
        stats.put("publishedEvents", outboxEventRepository.countByStatus(OutboxStatus.PUBLISHED));
        stats.put("failedEvents", outboxEventRepository.countByStatus(OutboxStatus.FAILED));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        List<ProcessedEvent> processedEvents = processedEventRepository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, safeLimit));

        KafkaStreamStatusResponse response = new KafkaStreamStatusResponse(
                kafkaEnabled,
                bootstrapServers,
                topics,
                stats,
                outboxEvents,
                processedEvents
        );

        return ResponseEntity.ok(response);
    }
}

