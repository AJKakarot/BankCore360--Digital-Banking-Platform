package com.bankcore360.service;

import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.repository.UserSessionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service managing enterprise data retention for transient operational records.
 *
 * CRITICAL FINANCIAL COMPLIANCE GUARANTEE:
 * Financial ledger entries (ledger_entries, ledger_transactions, account_balances,
 * transaction_reversals, bank_statements, reconciliation_batches) are permanently
 * IMMUTABLE under banking regulatory standards and are NEVER purged, pruned, or altered.
 *
 * Only transient, non-financial operational records (e.g., expired user sessions,
 * consumer idempotency markers past deduplication windows) are purged.
 */
@Service
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);

    @Value("${retention.session.days:180}")
    private int sessionRetentionDays;

    @Value("${retention.events.days:90}")
    private int eventRetentionDays;

    private final UserSessionRecordRepository userSessionRepository;
    private final ProcessedEventRepository processedEventRepository;

    public AuditLogRetentionService(UserSessionRecordRepository userSessionRepository,
                                   @Autowired(required = false) ProcessedEventRepository processedEventRepository) {
        this.userSessionRepository = userSessionRepository;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Nightly scheduled cleanup job for transient operational records.
     */
    @Scheduled(cron = "${retention.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public RetentionPurgeResult purgeExpiredOperationalData() {
        LocalDateTime sessionCutoff = LocalDateTime.now().minusDays(sessionRetentionDays);
        LocalDateTime eventCutoff = LocalDateTime.now().minusDays(eventRetentionDays);

        log.info("Starting operational data retention cleanup. Session cutoff: {}, Event cutoff: {}",
                sessionCutoff, eventCutoff);

        int prunedSessions = userSessionRepository.deleteInactiveSessionsOlderThan(sessionCutoff);
        log.info("Pruned {} inactive user session records older than {} days", prunedSessions, sessionRetentionDays);

        int prunedEvents = 0;
        if (processedEventRepository != null) {
            prunedEvents = processedEventRepository.deleteEventsOlderThan(eventCutoff);
            log.info("Pruned {} processed event deduplication markers older than {} days", prunedEvents, eventRetentionDays);
        }

        return new RetentionPurgeResult(prunedSessions, prunedEvents, sessionCutoff, eventCutoff);
    }

    public record RetentionPurgeResult(
            int prunedSessions,
            int prunedEvents,
            LocalDateTime sessionCutoff,
            LocalDateTime eventCutoff
    ) {}
}

