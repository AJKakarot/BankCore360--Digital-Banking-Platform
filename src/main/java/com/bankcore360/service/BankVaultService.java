package com.bankcore360.service;

import com.bankcore360.dto.InjectCapitalRequest;
import com.bankcore360.dto.VaultStatusResponse;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InsufficientBankLiquidityException;
import com.bankcore360.repository.BankAccountRepository;
import com.bankcore360.repository.BankVaultRepository;
import com.bankcore360.repository.VaultTransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class BankVaultService {

    private static final Logger log = LoggerFactory.getLogger(BankVaultService.class);
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final BankVaultRepository bankVaultRepository;
    private final VaultTransactionRepository vaultTransactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public BankVaultService(BankVaultRepository bankVaultRepository,
                            VaultTransactionRepository vaultTransactionRepository,
                            BankAccountRepository bankAccountRepository) {
        this.bankVaultRepository = bankVaultRepository;
        this.vaultTransactionRepository = vaultTransactionRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * Initializes the Central Bank Vault with ₹100 Lakh (₹1,00,00,000.00 / ₹1 Crore)
     * if it does not already exist in the database.
     */
    @PostConstruct
    @Transactional
    public void initVault() {
        if (!bankVaultRepository.existsById(BankVault.DEFAULT_VAULT_ID)) {
            BankVault vault = new BankVault(
                    BankVault.DEFAULT_VAULT_ID,
                    "BankCore360 Central Treasury & Reserve",
                    BankVault.DEFAULT_INITIAL_CAPITAL
            );
            bankVaultRepository.save(vault);

            // Record initial capital injection in ledger
            VaultTransaction initTxn = new VaultTransaction(
                    generateTransactionReference("CAP"),
                    "INIT-VAULT-RESERVE-100L",
                    VaultTransactionType.CAPITAL_INJECTION,
                    "CREDIT",
                    BankVault.DEFAULT_INITIAL_CAPITAL,
                    BigDecimal.ZERO,
                    BankVault.DEFAULT_INITIAL_CAPITAL,
                    "CENTRAL-TREASURY",
                    "VAULT-001",
                    "BankCore360 Core Vault",
                    "SYSTEM_INIT",
                    "Initial Core Banking Liquidity Reserve of ₹100 Lakh (₹1 Crore)"
            );
            vaultTransactionRepository.save(initTxn);
            log.info("🏦 Initialized BankCore360 Vault with initial reserve ₹{}", BankVault.DEFAULT_INITIAL_CAPITAL);
        }
    }

    /**
     * Atomic & Idempotent Loan Disbursement from the Bank Vault directly to Customer Account.
     * Uses PESSIMISTIC_WRITE lock on BankVault to prevent concurrency double-spending race conditions.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VaultTransaction disburseLoanFromVault(LoanApplication app, String operator) {
        String idempotencyKey = "DISBURSE-" + app.getApplicationReference();

        // 1. Idempotency Check: If already disbursed, return recorded transaction
        Optional<VaultTransaction> existing = vaultTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.warn("⚠️ Disbursement idempotency caught: Loan {} was already disbursed in Txn {}",
                    app.getApplicationReference(), existing.get().getTransactionReference());
            return existing.get();
        }

        BigDecimal loanAmount = app.getApprovedAmount();
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            loanAmount = app.getRequestedAmount();
        }

        // 2. Fetch Vault with Pessimistic Lock
        BankVault vault = bankVaultRepository.findByIdWithLock(BankVault.DEFAULT_VAULT_ID)
                .orElseGet(() -> {
                    BankVault nv = new BankVault(BankVault.DEFAULT_VAULT_ID, "BankCore360 Central Treasury & Reserve", BankVault.DEFAULT_INITIAL_CAPITAL);
                    return bankVaultRepository.save(nv);
                });

        // 3. Check Liquidity Guardrail
        if (vault.getAvailableLiquidity().compareTo(loanAmount) < 0) {
            throw new InsufficientBankLiquidityException(
                    "Insufficient Bank Vault Liquidity! Available: ₹" + vault.getAvailableLiquidity()
                            + ", Requested Loan Amount: ₹" + loanAmount + ". Please inject capital or reduce loan size."
            );
        }

        BigDecimal balanceBefore = vault.getAvailableLiquidity();
        BigDecimal balanceAfter = balanceBefore.subtract(loanAmount);

        // 4. Update Vault Balances Atomically
        vault.setAvailableLiquidity(balanceAfter);
        vault.setTotalDisbursed(vault.getTotalDisbursed().add(loanAmount));
        vault.setUpdatedAt(LocalDateTime.now());
        bankVaultRepository.save(vault);

        // 5. Credit Customer's Real Bank Account
        Customer customer = app.getCustomer();
        BankAccount customerAccount = resolveOrCreateCustomerAccount(customer);
        customerAccount.setBalance(customerAccount.getBalance().add(loanAmount));
        bankAccountRepository.save(customerAccount);

        // 6. Record Immutable Vault Ledger Transaction
        String txnRef = generateTransactionReference("DSB");
        VaultTransaction txn = new VaultTransaction(
                txnRef,
                idempotencyKey,
                VaultTransactionType.LOAN_DISBURSAL,
                "DEBIT",
                loanAmount,
                balanceBefore,
                balanceAfter,
                app.getApplicationReference(),
                customerAccount.getAccountNumber(),
                customer != null ? customer.getFullName() : "Valued Customer",
                operator != null ? operator : "CREDIT_MANAGER",
                "Loan Disbursed for " + app.getLoanType() + " Loan (Ref: " + app.getApplicationReference() + ") to A/C " + customerAccount.getAccountNumber()
        );

        VaultTransaction savedTxn = vaultTransactionRepository.save(txn);
        log.info("💸 [DISBURSEMENT ATOMIC] Deducted ₹{} from Vault (Remaining: ₹{}). Credited to Account {} (New Bal: ₹{})",
                loanAmount, balanceAfter, customerAccount.getAccountNumber(), customerAccount.getBalance());

        return savedTxn;
    }

    /**
     * Admin Capital Injection to increase bank vault liquidity pool.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VaultTransaction injectCapital(InjectCapitalRequest request, String adminUsername) {
        BigDecimal amount = request.getAmount();

        BankVault vault = bankVaultRepository.findByIdWithLock(BankVault.DEFAULT_VAULT_ID)
                .orElseGet(() -> {
                    BankVault nv = new BankVault(BankVault.DEFAULT_VAULT_ID, "BankCore360 Central Treasury & Reserve", BankVault.DEFAULT_INITIAL_CAPITAL);
                    return bankVaultRepository.save(nv);
                });

        BigDecimal balanceBefore = vault.getAvailableLiquidity();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        vault.setTotalCapital(vault.getTotalCapital().add(amount));
        vault.setAvailableLiquidity(balanceAfter);
        vault.setUpdatedAt(LocalDateTime.now());
        bankVaultRepository.save(vault);

        String txnRef = generateTransactionReference("INJ");
        String idempotencyKey = "INJECT-" + txnRef;

        VaultTransaction txn = new VaultTransaction(
                txnRef,
                idempotencyKey,
                VaultTransactionType.CAPITAL_INJECTION,
                "CREDIT",
                amount,
                balanceBefore,
                balanceAfter,
                txnRef,
                "VAULT-001",
                "Central Liquidity Pool",
                adminUsername != null ? adminUsername : "ADMIN",
                request.getRemarks() != null && !request.getRemarks().isBlank()
                        ? request.getRemarks()
                        : "Capital injection of ₹" + amount + " by " + adminUsername
        );

        log.info("💰 [CAPITAL INJECTION] Added ₹{} to Bank Vault by {}. New Vault Liquidity: ₹{}",
                amount, adminUsername, balanceAfter);

        return vaultTransactionRepository.save(txn);
    }

    /**
     * Returns live status of Bank Vault, balances, utilization %, and recent 20 ledger events.
     */
    @Transactional(readOnly = true)
    public VaultStatusResponse getVaultStatus() {
        BankVault vault = bankVaultRepository.findById(BankVault.DEFAULT_VAULT_ID)
                .orElseGet(() -> new BankVault(BankVault.DEFAULT_VAULT_ID, "BankCore360 Central Treasury & Reserve", BankVault.DEFAULT_INITIAL_CAPITAL));

        double utilization = 0.0;
        if (vault.getTotalCapital().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal disbursedRatio = vault.getTotalDisbursed()
                    .multiply(new BigDecimal("100"))
                    .divide(vault.getTotalCapital(), 2, RoundingMode.HALF_UP);
            utilization = disbursedRatio.doubleValue();
        }

        List<VaultTransaction> recent = vaultTransactionRepository.findTop20ByOrderByTimestampDesc();

        return new VaultStatusResponse(
                vault.getVaultName(),
                vault.getTotalCapital(),
                vault.getAvailableLiquidity(),
                vault.getTotalDisbursed(),
                vault.getTotalRepaid(),
                utilization,
                vault.getUpdatedAt(),
                recent
        );
    }

    /**
     * Finds existing BankAccount or provisions a new standard Savings account for customer.
     */
    private BankAccount resolveOrCreateCustomerAccount(Customer customer) {
        if (customer != null && customer.getId() != null) {
            List<BankAccount> accounts = bankAccountRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
            if (!accounts.isEmpty()) {
                return accounts.get(0);
            }
        }

        String accountNumber = "3601" + String.format("%08d", customer != null && customer.getId() != null ? customer.getId() : 1);
        Optional<BankAccount> byNum = bankAccountRepository.findByAccountNumber(accountNumber);
        if (byNum.isPresent()) {
            return byNum.get();
        }

        BankAccount newAccount = new BankAccount(
                accountNumber,
                customer,
                AccountType.SAVINGS,
                BigDecimal.ZERO,
                AccountStatus.ACTIVE
        );
        return bankAccountRepository.save(newAccount);
    }

    private String generateTransactionReference(String prefix) {
        int year = Year.now().getValue();
        StringBuilder sb = new StringBuilder("TXN-").append(prefix).append("-").append(year).append("-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
