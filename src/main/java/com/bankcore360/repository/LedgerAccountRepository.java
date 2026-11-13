package com.bankcore360.repository;

import com.bankcore360.entity.LedgerAccount;
import com.bankcore360.entity.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<LedgerAccount> findByCustomerId(Long customerId);

    List<LedgerAccount> findByAccountType(LedgerAccountType accountType);

    Optional<LedgerAccount> findByNameAndIsSystemAccountTrue(String name);
}

