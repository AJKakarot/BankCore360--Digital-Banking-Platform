package com.bankcore360.repository;

import com.bankcore360.entity.KycDocumentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentAuditLogRepository extends JpaRepository<KycDocumentAuditLog, Long> {

    List<KycDocumentAuditLog> findByDocumentIdOrderByTimestampAsc(Long documentId);

    List<KycDocumentAuditLog> findByCustomerIdOrderByTimestampAsc(Long customerId);
}

