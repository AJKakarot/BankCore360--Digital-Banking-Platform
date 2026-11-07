package com.bankcore360.repository;

import com.bankcore360.entity.BankVault;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BankVaultRepository extends JpaRepository<BankVault, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM BankVault v WHERE v.id = :id")
    Optional<BankVault> findByIdWithLock(@Param("id") Long id);
}
