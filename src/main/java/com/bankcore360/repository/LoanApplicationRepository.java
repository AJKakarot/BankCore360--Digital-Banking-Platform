package com.bankcore360.repository;

import com.bankcore360.entity.Customer;
import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationReference(String applicationReference);

    boolean existsByApplicationReference(String applicationReference);

    List<LoanApplication> findByCustomerOrderBySubmittedAtDesc(Customer customer);

    List<LoanApplication> findByStatusInOrderBySubmittedAtAsc(Collection<LoanStatus> statuses);

    List<LoanApplication> findByStatusOrderBySubmittedAtAsc(LoanStatus status);

    List<LoanApplication> findAllByOrderBySubmittedAtDesc();

    long countByStatus(LoanStatus status);

    @Query("SELECT COALESCE(SUM(l.approvedAmount), 0) FROM LoanApplication l WHERE l.status IN ('APPROVED', 'DISBURSED')")
    BigDecimal sumApprovedLoanAmount();

    @Query("SELECT COALESCE(SUM(l.approvedAmount), 0) FROM LoanApplication l WHERE l.status = 'DISBURSED'")
    BigDecimal sumDisbursedLoanAmount();
}

