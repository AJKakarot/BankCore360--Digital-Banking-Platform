package com.bankcore360.repository;

import com.bankcore360.entity.FraudAlert;
import com.bankcore360.entity.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Optional<FraudAlert> findByAlertReference(String alertReference);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlertStatus status, Pageable pageable);

    List<FraudAlert> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(FraudAlertStatus status);
}
