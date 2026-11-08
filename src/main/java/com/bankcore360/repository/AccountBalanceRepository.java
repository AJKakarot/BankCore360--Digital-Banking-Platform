package com.bankcore360.repository;

import com.bankcore360.entity.AccountBalance;
import com.bankcore360.entity.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    Optional<AccountBalance> findByAccount(LedgerAccount account);

    Optional<AccountBalance> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AccountBalance b WHERE b.account.id = :accountId")
    Optional<AccountBalance> findByAccountIdWithLock(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AccountBalance b WHERE b.account.accountNumber = :accountNumber")
    Optional<AccountBalance> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}
