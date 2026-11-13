package com.bankcore360.repository;

import com.bankcore360.entity.FraudAssessment;
import com.bankcore360.entity.FraudRiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long> {

    Optional<FraudAssessment> findByAssessmentReference(String assessmentReference);

    Page<FraudAssessment> findByAccountNumberOrderByEvaluatedAtDesc(String accountNumber, Pageable pageable);

    Page<FraudAssessment> findByRiskLevelOrderByEvaluatedAtDesc(FraudRiskLevel riskLevel, Pageable pageable);
}
