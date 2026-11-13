package com.bankcore360.repository;

import com.bankcore360.entity.DiscrepancyStatus;
import com.bankcore360.entity.DiscrepancyType;
import com.bankcore360.entity.ReconciliationDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, Long> {

    List<ReconciliationDiscrepancy> findByBatchId(Long batchId);

    List<ReconciliationDiscrepancy> findByStatus(DiscrepancyStatus status);

    List<ReconciliationDiscrepancy> findByDiscrepancyType(DiscrepancyType type);
}
