package com.bankcore360.repository;

import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.LoanApplication;
import com.bankcore360.entity.LoanDocument;
import com.bankcore360.entity.LoanDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanApplicationOrderByUploadedAtDesc(LoanApplication loanApplication);

    List<LoanDocument> findByLoanApplicationIdOrderByUploadedAtDesc(Long loanApplicationId);

    List<LoanDocument> findByLoanApplicationIdAndDocumentStatus(Long loanApplicationId, DocumentVerificationStatus status);

    long countByLoanApplicationIdAndDocumentStatus(Long loanApplicationId, DocumentVerificationStatus status);

    long countByLoanApplicationId(Long loanApplicationId);

    boolean existsByLoanApplicationIdAndDocumentType(Long loanApplicationId, LoanDocumentType documentType);
}

