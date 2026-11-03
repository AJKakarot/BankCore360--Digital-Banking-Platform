package com.bankcore360.service;

import com.bankcore360.dto.LoanAuditLogResponse;
import com.bankcore360.entity.LoanAuditLog;
import com.bankcore360.repository.LoanAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanAuditService {

    private final LoanAuditLogRepository loanAuditLogRepository;

    public LoanAuditService(LoanAuditLogRepository loanAuditLogRepository) {
        this.loanAuditLogRepository = loanAuditLogRepository;
    }

    @Transactional
    public void logAction(String applicationReference, String actorUsername, String actorRole,
                          String action, String previousStatus, String newStatus, String remarks) {
        LoanAuditLog log = new LoanAuditLog(
                applicationReference,
                actorUsername != null ? actorUsername : "SYSTEM",
                actorRole != null ? actorRole : "SYSTEM",
                action,
                previousStatus,
                newStatus,
                remarks
        );
        loanAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<LoanAuditLogResponse> getAuditHistory(String applicationReference) {
        return loanAuditLogRepository.findByApplicationReferenceOrderByTimestampAsc(applicationReference)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LoanAuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return loanAuditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::mapToResponse);
    }

    private LoanAuditLogResponse mapToResponse(LoanAuditLog log) {
        return new LoanAuditLogResponse(
                log.getId(),
                log.getApplicationReference(),
                log.getActorUsername(),
                log.getActorRole(),
                log.getAction(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getRemarks(),
                log.getTimestamp()
        );
    }
}

