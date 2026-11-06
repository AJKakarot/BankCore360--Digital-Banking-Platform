package com.bankcore360.repository;

import com.bankcore360.entity.AccountAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, Long> {

    List<AccountAuditLog> findByAccountNumberOrderByTimestampDesc(String accountNumber);

    List<AccountAuditLog> findByCustomerIdOrderByTimestampDesc(Long customerId);
}

