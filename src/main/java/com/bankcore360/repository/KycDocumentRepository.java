package com.bankcore360.repository;

import com.bankcore360.entity.DocumentVerificationStatus;
import com.bankcore360.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByCustomerIdOrderByUploadedAtDesc(Long customerId);

    List<KycDocument> findByVerificationStatusOrderByUploadedAtAsc(DocumentVerificationStatus verificationStatus);

    Optional<KycDocument> findByIdAndCustomerId(Long id, Long customerId);
}

