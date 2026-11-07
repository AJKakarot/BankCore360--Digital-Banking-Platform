package com.bankcore360.repository;

import com.bankcore360.entity.VaultTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VaultTransactionRepository extends JpaRepository<VaultTransaction, Long> {

    Optional<VaultTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<VaultTransaction> findByRelatedReference(String relatedReference);

    List<VaultTransaction> findTop20ByOrderByTimestampDesc();

    Page<VaultTransaction> findAllByOrderByTimestampDesc(Pageable pageable);

    List<VaultTransaction> findByBeneficiaryAccountNumberOrderByTimestampDesc(String accountNumber);
}

