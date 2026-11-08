package com.bankcore360.repository;

import com.bankcore360.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findByEntryReference(String entryReference);

    List<LedgerEntry> findByTransactionIdOrderByEntryOrderAsc(Long transactionId);

    List<LedgerEntry> findByAccountIdOrderByIdDesc(Long accountId);

    Page<LedgerEntry> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);

    List<LedgerEntry> findByAccountAccountNumberOrderByIdDesc(String accountNumber);

    Page<LedgerEntry> findByAccountAccountNumberOrderByIdDesc(String accountNumber, Pageable pageable);
}
