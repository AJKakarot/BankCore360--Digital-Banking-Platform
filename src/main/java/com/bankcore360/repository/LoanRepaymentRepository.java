package com.bankcore360.repository;

import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanRepayment;
import com.bankcore360.entity.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanApplicationOrderByInstallmentNumberAsc(LoanApplication loanApplication);

    List<LoanRepayment> findByLoanApplicationApplicationReferenceOrderByInstallmentNumberAsc(String applicationReference);

    long countByLoanApplicationIdAndStatus(Long loanApplicationId, RepaymentStatus status);
}

