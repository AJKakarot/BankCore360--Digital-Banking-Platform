package com.bankcore360.repository;

import com.bankcore360.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

    Optional<BankStatement> findByStatementReference(String statementReference);

    boolean existsByFileHashSha256(String fileHashSha256);
}

