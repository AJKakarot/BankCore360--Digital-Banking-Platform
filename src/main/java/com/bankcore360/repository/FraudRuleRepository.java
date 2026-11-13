package com.bankcore360.repository;

import com.bankcore360.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findByIsActiveTrue();

    Optional<FraudRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);
}

