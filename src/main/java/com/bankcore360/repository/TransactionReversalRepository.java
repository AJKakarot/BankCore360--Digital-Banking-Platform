package com.bankcore360.repository;

import com.bankcore360.entity.TransactionReversal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionReversalRepository extends JpaRepository<TransactionReversal, Long> {

    Optional<TransactionReversal> findByOriginalTransactionId(Long originalTransactionId);

    Optional<TransactionReversal> findByReversalTransactionId(Long reversalTransactionId);

    boolean existsByOriginalTransactionId(Long originalTransactionId);
}
