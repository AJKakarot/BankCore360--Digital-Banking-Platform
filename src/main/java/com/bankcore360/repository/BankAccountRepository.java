package com.bankcore360.repository;

import com.bankcore360.entity.AccountStatus;
import com.bankcore360.entity.AccountType;
import com.bankcore360.entity.BankAccount;
import com.bankcore360.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByCustomerOrderByCreatedAtDesc(Customer customer);

    List<BankAccount> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerIdAndAccountTypeAndAccountStatusIn(
            Long customerId, AccountType accountType, Collection<AccountStatus> statuses
    );
}

