package com.bankcore360.repository;

import com.bankcore360.entity.FraudInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudInvestigationRepository extends JpaRepository<FraudInvestigation, Long> {

    List<FraudInvestigation> findByAlertIdOrderByCreatedAtDesc(Long alertId);
}

