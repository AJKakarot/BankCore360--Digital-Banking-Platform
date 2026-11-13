package com.bankcore360.repository;

import com.bankcore360.entity.BankStatementItem;
import com.bankcore360.entity.ReconciliationItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankStatementItemRepository extends JpaRepository<BankStatementItem, Long> {

    List<BankStatementItem> findByStatementId(Long statementId);

    List<BankStatementItem> findByStatementIdAndReconciliationStatus(Long statementId, ReconciliationItemStatus status);
}
