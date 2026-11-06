package com.bankcore360.repository;

import com.bankcore360.entity.KycAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KycAuditLogRepository extends JpaRepository<KycAuditLog, Long> {

    List<KycAuditLog> findByCustomerIdOrderByCreatedAtAscIdAsc(Long customerId);

    List<KycAuditLog> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    List<KycAuditLog> findByCustomerId(Long customerId);

    @Query("SELECT k FROM KycAuditLog k WHERE k.customerId = :customerId ORDER BY k.createdAt ASC, k.id ASC")
    List<KycAuditLog> findByCustomerIdOrderByTimestampAscIdAsc(@Param("customerId") Long customerId);
}
