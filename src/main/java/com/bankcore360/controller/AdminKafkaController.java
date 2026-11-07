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
@Tag(name = "Admin Event Stream & Outbox Monitor", description = "Endpoints for inspecting transactional event streams and audit history")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKafkaController {

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;

    public AdminKafkaController(
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @GetMapping("/events")
    @Operation(summary = "Get Event stream, Outbox event records, and audit status",
            description = "Returns real-time transactional outbox records and audit history")
    public ResponseEntity<KafkaStreamStatusResponse> getKafkaStreamStatus(
            @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.min(100, Math.max(10, limit));

        List<TopicDetail> topics = List.of(
                new TopicDetail("loan-events", "Audit Pipeline", "Loan application creation, updates, and workflow changes"),
                new TopicDetail("kyc-events", "Audit Pipeline", "Customer KYC document uploads, review, verify, and reject events"),
                new TopicDetail("approval-events", "Audit Pipeline", "Credit manager loan approval, rejection, and sanction decisions"),
                new TopicDetail("repayment-events", "Accounting Pipeline", "EMI repayment schedules, payments, and installment notices"),
                new TopicDetail("notification-events", "Notification Pipeline", "Real-time user notification alerts and communications")
        );

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalEvents", outboxEventRepository.count());
        stats.put("pendingEvents", outboxEventRepository.countByStatus(OutboxStatus.PENDING));
        stats.put("publishedEvents", outboxEventRepository.countByStatus(OutboxStatus.PUBLISHED));
        stats.put("failedEvents", outboxEventRepository.countByStatus(OutboxStatus.FAILED));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        List<ProcessedEvent> processedEvents = processedEventRepository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, safeLimit));

        KafkaStreamStatusResponse response = new KafkaStreamStatusResponse(
                false,
                "embedded-database-stream",
                topics,
                stats,
                outboxEvents,
                processedEvents
        );

        return ResponseEntity.ok(response);
    }
}

