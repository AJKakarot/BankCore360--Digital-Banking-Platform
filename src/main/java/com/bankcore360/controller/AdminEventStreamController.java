package com.bankcore360.controller;

import com.bankcore360.dto.EventStreamStatusResponse;
import com.bankcore360.dto.EventStreamStatusResponse.ChannelDetail;
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
@RequestMapping("/api/admin/events")
@Tag(name = "Admin Event Stream & Outbox Monitor", description = "Endpoints for inspecting transactional event streams and audit history")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventStreamController {

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;

    public AdminEventStreamController(
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @GetMapping({"", "/", "/status", "/events"})
    @Operation(summary = "Get Event stream, Outbox event records, and audit status",
            description = "Returns real-time transactional outbox records and audit history")
    public ResponseEntity<EventStreamStatusResponse> getEventStreamStatus(
            @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.min(100, Math.max(10, limit));

        List<ChannelDetail> channels = List.of(
                new ChannelDetail("loan-events", "Audit Pipeline", "Loan application creation, updates, and workflow changes"),
                new ChannelDetail("kyc-events", "Audit Pipeline", "Customer KYC document uploads, review, verify, and reject events"),
                new ChannelDetail("approval-events", "Audit Pipeline", "Credit manager loan approval, rejection, and sanction decisions"),
                new ChannelDetail("repayment-events", "Accounting Pipeline", "EMI repayment schedules, payments, and installment notices"),
                new ChannelDetail("notification-events", "Notification Pipeline", "Real-time user notification alerts and communications")
        );

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalEvents", outboxEventRepository.count());
        stats.put("pendingEvents", outboxEventRepository.countByStatus(OutboxStatus.PENDING));
        stats.put("publishedEvents", outboxEventRepository.countByStatus(OutboxStatus.PUBLISHED));
        stats.put("failedEvents", outboxEventRepository.countByStatus(OutboxStatus.FAILED));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        List<ProcessedEvent> processedEvents = processedEventRepository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, safeLimit));

        EventStreamStatusResponse response = new EventStreamStatusResponse(
                true,
                "Transactional Outbox Engine (PostgreSQL)",
                channels,
                stats,
                outboxEvents,
                processedEvents
        );

        return ResponseEntity.ok(response);
    }
}
