package com.bankcore360.repository;

import com.bankcore360.entity.LoanAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanAuditLogRepository extends JpaRepository<LoanAuditLog, Long> {

    List<LoanAuditLog> findByApplicationReferenceOrderByTimestampAsc(String applicationReference);

    Page<LoanAuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}

