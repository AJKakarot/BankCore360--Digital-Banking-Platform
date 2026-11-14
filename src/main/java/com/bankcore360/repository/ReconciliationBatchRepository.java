package com.bankcore360.repository;

import com.bankcore360.entity.ReconciliationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationBatchRepository extends JpaRepository<ReconciliationBatch, Long> {

    Optional<ReconciliationBatch> findByBatchReference(String batchReference);

    List<ReconciliationBatch> findByStatementIdOrderByReconciledAtDesc(Long statementId);
}

