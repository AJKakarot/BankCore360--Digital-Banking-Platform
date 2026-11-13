package com.bankcore360.service;

import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.exception.*;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    public static final String SYSTEM_VAULT_ACCOUNT_NUMBER = "SYS-VAULT-001";
    public static final String SYSTEM_VAULT_NAME = "BANK_CENTRAL_VAULT";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionReversalRepository transactionReversalRepository;
    private final CustomerRepository customerRepository;
    private FintechMetricsService fintechMetricsService;

    public LedgerService(LedgerAccountRepository ledgerAccountRepository,
                         AccountBalanceRepository accountBalanceRepository,
                         LedgerTransactionRepository ledgerTransactionRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         TransactionReversalRepository transactionReversalRepository,
                         CustomerRepository customerRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionReversalRepository = transactionReversalRepository;
        this.customerRepository = customerRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setFintechMetricsService(FintechMetricsService fintechMetricsService) {
        this.fintechMetricsService = fintechMetricsService;
    }

    /**
     * Ensures the central Bank Vault system account exists and returns its balance.
     */
    @Transactional
    public LedgerAccount getOrCreateSystemVaultAccount() {
        return ledgerAccountRepository.findByAccountNumber(SYSTEM_VAULT_ACCOUNT_NUMBER)
                .orElseGet(() -> {
                    log.info("Provisioning central double-entry system vault account [{}]", SYSTEM_VAULT_ACCOUNT_NUMBER);
                    LedgerAccount vault = new LedgerAccount(
                            SYSTEM_VAULT_ACCOUNT_NUMBER,
                            SYSTEM_VAULT_NAME,
                            LedgerAccountType.ASSET,
                            "INR",
                            LedgerAccountStatus.ACTIVE,
                            null,
                            true
                    );
                    LedgerAccount saved = ledgerAccountRepository.save(vault);
                    AccountBalance balance = new AccountBalance(saved);
                    // Central vault starts with 100 Lakh (10,000,000 INR) liquidity pool
                    BigDecimal initialLiquidity = new BigDecimal("10000000.00");
                    balance.setCurrentBalance(initialLiquidity);
                    balance.setAvailableBalance(initialLiquidity);
                    accountBalanceRepository.save(balance);
                    return saved;
                });
    }

    /**
     * Create a new ledger account with an initialized zero balance.
     */
    @Transactional
    public LedgerAccountResponse createAccount(CreateLedgerAccountRequest request) {
        if (ledgerAccountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalLedgerOperationException("Ledger account number already exists: " + request.getAccountNumber());
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        LedgerAccount account = new LedgerAccount(
                request.getAccountNumber(),
                request.getName(),
                request.getAccountType(),
                request.getCurrency(),
                LedgerAccountStatus.ACTIVE,
                customer,
                request.isSystemAccount()
        );
        LedgerAccount savedAccount = ledgerAccountRepository.save(account);

        AccountBalance balance = new AccountBalance(savedAccount);
        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            balance.setCurrentBalance(request.getInitialBalance());
            balance.setAvailableBalance(request.getInitialBalance());
        }
        AccountBalance savedBalance = accountBalanceRepository.save(balance);

        return new LedgerAccountResponse(savedAccount, savedBalance.getCurrentBalance(), savedBalance.getAvailableBalance());
    }

    /**
     * Retrieve a ledger account by its primary ID.
     */
    @Transactional(readOnly = true)
    public LedgerAccountResponse getAccountById(Long id) {
        LedgerAccount account = ledgerAccountRepository.findById(id)
                .orElseThrow(() -> new LedgerAccountNotFoundException("Ledger account not found with id: " + id));
        AccountBalance balance = accountBalanceRepository.findByAccountId(id)
                .orElse(new AccountBalance(account));
        return new LedgerAccountResponse(account, balance.getCurrentBalance(), balance.getAvailableBalance());
    }

    /**
     * Retrieve a ledger account by its unique account number.
     */
    @Transactional(readOnly = true)
    public LedgerAccountResponse getAccountByNumber(String accountNumber) {
        LedgerAccount account = ledgerAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new LedgerAccountNotFoundException("Ledger account not found: " + accountNumber));
        AccountBalance balance = accountBalanceRepository.findByAccountId(account.getId())
                .orElse(new AccountBalance(account));
        return new LedgerAccountResponse(account, balance.getCurrentBalance(), balance.getAvailableBalance());
    }

    /**
     * List all ledger accounts with pagination.
     */
    @Transactional(readOnly = true)
    public Page<LedgerAccountResponse> getAllAccounts(Pageable pageable) {
        return ledgerAccountRepository.findAll(pageable)
                .map(account -> {
                    AccountBalance bal = accountBalanceRepository.findByAccountId(account.getId())
                            .orElse(new AccountBalance(account));
                    return new LedgerAccountResponse(account, bal.getCurrentBalance(), bal.getAvailableBalance());
                });
    }

    /**
     * Record a Deposit into a Customer Ledger Account.
     * Double-Entry Rule:
     * - DEBIT: Bank Vault Asset Account (Asset increases)
     * - CREDIT: Customer Account (Liability increases)
     * Total Debit == Total Credit
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LedgerTransactionResponse deposit(LedgerDepositRequest request, String initiatedBy) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalLedgerOperationException("Deposit amount must be strictly greater than zero");
        }

        // Idempotency check
        Optional<LedgerTransaction> existingTxn = ledgerTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            log.info("Idempotent deposit hit for key: {}", request.getIdempotencyKey());
            return new LedgerTransactionResponse(existingTxn.get());
        }

        LedgerAccount custAccount = ledgerAccountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new LedgerAccountNotFoundException("Destination account not found: " + request.getAccountNumber()));
        if (custAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new IllegalLedgerOperationException("Cannot deposit into inactive or frozen account: " + custAccount.getStatus());
        }

        LedgerAccount vaultAccount = getOrCreateSystemVaultAccount();

        // Lock accounts in strictly ascending ID order to prevent deadlocks
        AccountBalance custBalance;
        AccountBalance vaultBalance;
        if (custAccount.getId() < vaultAccount.getId()) {
            custBalance = getBalanceWithLock(custAccount.getId());
            vaultBalance = getBalanceWithLock(vaultAccount.getId());
        } else {
            vaultBalance = getBalanceWithLock(vaultAccount.getId());
            custBalance = getBalanceWithLock(custAccount.getId());
        }

        String txnRef = generateTransactionReference("DEP");
        LedgerTransaction txn = new LedgerTransaction(
                txnRef,
                request.getIdempotencyKey(),
                LedgerTransactionType.DEPOSIT,
                request.getAmount(),
                custAccount.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Cash/Bank Deposit",
                initiatedBy
        );
        LedgerTransaction savedTxn = ledgerTransactionRepository.save(txn);
        if (fintechMetricsService != null) {
            fintechMetricsService.recordLedgerTransaction("DEPOSIT", "POSTED", request.getAmount());
        }

        // Vault: ASSET normal debit -> DEBIT increases vault asset
        BigDecimal vaultNewBal = applyEntryToBalance(vaultAccount.getAccountType(), vaultBalance.getCurrentBalance(),
                LedgerEntryDirection.DEBIT, request.getAmount());
        vaultBalance.setCurrentBalance(vaultNewBal);
        vaultBalance.setAvailableBalance(vaultNewBal);
        vaultBalance.setLastTransactionId(savedTxn.getId());
        vaultBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(vaultBalance);

        LedgerEntry vaultEntry = new LedgerEntry(
                generateEntryReference(txnRef, 1),
                vaultAccount,
                LedgerEntryDirection.DEBIT,
                request.getAmount(),
                vaultNewBal,
                1,
                "Vault Cash Inflow for Deposit " + txnRef
        );
        vaultEntry.setTransaction(savedTxn);

        // Customer: LIABILITY normal credit -> CREDIT increases customer liability
        BigDecimal custNewBal = applyEntryToBalance(custAccount.getAccountType(), custBalance.getCurrentBalance(),
                LedgerEntryDirection.CREDIT, request.getAmount());
        custBalance.setCurrentBalance(custNewBal);
        custBalance.setAvailableBalance(custNewBal);
        custBalance.setLastTransactionId(savedTxn.getId());
        custBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(custBalance);

        LedgerEntry custEntry = new LedgerEntry(
                generateEntryReference(txnRef, 2),
                custAccount,
                LedgerEntryDirection.CREDIT,
                request.getAmount(),
                custNewBal,
                2,
                "Deposit Credit to " + custAccount.getAccountNumber()
        );
        custEntry.setTransaction(savedTxn);

        List<LedgerEntry> entries = List.of(vaultEntry, custEntry);
        validateBalancedTransaction(entries);

        ledgerEntryRepository.saveAll(entries);
        savedTxn.setEntries(new ArrayList<>(entries));

        log.info("Deposit completed: txnRef={}, amount={}, custAccount={}", txnRef, request.getAmount(), custAccount.getAccountNumber());
        return new LedgerTransactionResponse(savedTxn);
    }

    /**
     * Record a Withdrawal from a Customer Ledger Account.
     * Double-Entry Rule:
     * - DEBIT: Customer Account (Liability decreases)
     * - CREDIT: Bank Vault Asset Account (Asset decreases)
     * Total Debit == Total Credit
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LedgerTransactionResponse withdraw(LedgerWithdrawRequest request, String initiatedBy) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalLedgerOperationException("Withdrawal amount must be strictly greater than zero");
        }

        // Idempotency check
        Optional<LedgerTransaction> existingTxn = ledgerTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            log.info("Idempotent withdraw hit for key: {}", request.getIdempotencyKey());
            return new LedgerTransactionResponse(existingTxn.get());
        }

        LedgerAccount custAccount = ledgerAccountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new LedgerAccountNotFoundException("Source account not found: " + request.getAccountNumber()));
        if (custAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new IllegalLedgerOperationException("Cannot withdraw from inactive or frozen account: " + custAccount.getStatus());
        }

        LedgerAccount vaultAccount = getOrCreateSystemVaultAccount();

        // Lock accounts in strictly ascending ID order
        AccountBalance custBalance;
        AccountBalance vaultBalance;
        if (custAccount.getId() < vaultAccount.getId()) {
            custBalance = getBalanceWithLock(custAccount.getId());
            vaultBalance = getBalanceWithLock(vaultAccount.getId());
        } else {
            vaultBalance = getBalanceWithLock(vaultAccount.getId());
            custBalance = getBalanceWithLock(custAccount.getId());
        }

        // Insufficient funds check for non-system accounts
        if (!custAccount.isSystemAccount() && custBalance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientLedgerFundsException(String.format(
                    "Insufficient funds in account %s. Available: %s, Requested: %s",
                    custAccount.getAccountNumber(), custBalance.getAvailableBalance(), request.getAmount()
            ));
        }

        String txnRef = generateTransactionReference("WTD");
        LedgerTransaction txn = new LedgerTransaction(
                txnRef,
                request.getIdempotencyKey(),
                LedgerTransactionType.WITHDRAWAL,
                request.getAmount(),
                custAccount.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Cash/ATM Withdrawal",
                initiatedBy
        );
        LedgerTransaction savedTxn = ledgerTransactionRepository.save(txn);
        if (fintechMetricsService != null) {
            fintechMetricsService.recordLedgerTransaction("WITHDRAWAL", "POSTED", request.getAmount());
        }

        // Customer: LIABILITY normal credit -> DEBIT decreases customer liability
        BigDecimal custNewBal = applyEntryToBalance(custAccount.getAccountType(), custBalance.getCurrentBalance(),
                LedgerEntryDirection.DEBIT, request.getAmount());
        custBalance.setCurrentBalance(custNewBal);
        custBalance.setAvailableBalance(custNewBal);
        custBalance.setLastTransactionId(savedTxn.getId());
        custBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(custBalance);

        LedgerEntry custEntry = new LedgerEntry(
                generateEntryReference(txnRef, 1),
                custAccount,
                LedgerEntryDirection.DEBIT,
                request.getAmount(),
                custNewBal,
                1,
                "Withdrawal Debit from " + custAccount.getAccountNumber()
        );
        custEntry.setTransaction(savedTxn);

        // Vault: ASSET normal debit -> CREDIT decreases vault asset
        BigDecimal vaultNewBal = applyEntryToBalance(vaultAccount.getAccountType(), vaultBalance.getCurrentBalance(),
                LedgerEntryDirection.CREDIT, request.getAmount());
        vaultBalance.setCurrentBalance(vaultNewBal);
        vaultBalance.setAvailableBalance(vaultNewBal);
        vaultBalance.setLastTransactionId(savedTxn.getId());
        vaultBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(vaultBalance);

        LedgerEntry vaultEntry = new LedgerEntry(
                generateEntryReference(txnRef, 2),
                vaultAccount,
                LedgerEntryDirection.CREDIT,
                request.getAmount(),
                vaultNewBal,
                2,
                "Vault Cash Outflow for Withdrawal " + txnRef
        );
        vaultEntry.setTransaction(savedTxn);

        List<LedgerEntry> entries = List.of(custEntry, vaultEntry);
        validateBalancedTransaction(entries);

        ledgerEntryRepository.saveAll(entries);
        savedTxn.setEntries(new ArrayList<>(entries));

        log.info("Withdrawal completed: txnRef={}, amount={}, custAccount={}", txnRef, request.getAmount(), custAccount.getAccountNumber());
        return new LedgerTransactionResponse(savedTxn);
    }

    /**
     * Record a Fund Transfer between two Ledger Accounts.
     * Double-Entry Rule:
     * - DEBIT: Source Account (Sender balance decreases)
     * - CREDIT: Destination Account (Recipient balance increases)
     * Total Debit == Total Credit
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LedgerTransactionResponse transfer(LedgerTransferRequest request, String initiatedBy) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalLedgerOperationException("Transfer amount must be strictly greater than zero");
        }
        if (request.getSourceAccountNumber().equalsIgnoreCase(request.getDestinationAccountNumber())) {
            throw new IllegalLedgerOperationException("Source and destination accounts cannot be identical");
        }

        // Idempotency check
        Optional<LedgerTransaction> existingTxn = ledgerTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            log.info("Idempotent transfer hit for key: {}", request.getIdempotencyKey());
            return new LedgerTransactionResponse(existingTxn.get());
        }

        LedgerAccount srcAccount = ledgerAccountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new LedgerAccountNotFoundException("Source account not found: " + request.getSourceAccountNumber()));
        LedgerAccount destAccount = ledgerAccountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new LedgerAccountNotFoundException("Destination account not found: " + request.getDestinationAccountNumber()));

        if (srcAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new IllegalLedgerOperationException("Source account is not active: " + srcAccount.getStatus());
        }
        if (destAccount.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new IllegalLedgerOperationException("Destination account is not active: " + destAccount.getStatus());
        }

        // Deadlock-free locking in strictly ascending ID order
        AccountBalance srcBalance;
        AccountBalance destBalance;
        if (srcAccount.getId() < destAccount.getId()) {
            srcBalance = getBalanceWithLock(srcAccount.getId());
            destBalance = getBalanceWithLock(destAccount.getId());
        } else {
            destBalance = getBalanceWithLock(destAccount.getId());
            srcBalance = getBalanceWithLock(srcAccount.getId());
        }

        // Verify sender has sufficient funds
        if (!srcAccount.isSystemAccount() && srcBalance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientLedgerFundsException(String.format(
                    "Insufficient funds in source account %s. Available: %s, Required: %s",
                    srcAccount.getAccountNumber(), srcBalance.getAvailableBalance(), request.getAmount()
            ));
        }

        String txnRef = generateTransactionReference("TRF");
        LedgerTransaction txn = new LedgerTransaction(
                txnRef,
                request.getIdempotencyKey(),
                LedgerTransactionType.TRANSFER,
                request.getAmount(),
                srcAccount.getCurrency(),
                request.getDescription() != null ? request.getDescription() : "Account-to-Account Transfer",
                initiatedBy
        );
        LedgerTransaction savedTxn = ledgerTransactionRepository.save(txn);
        if (fintechMetricsService != null) {
            fintechMetricsService.recordLedgerTransaction("TRANSFER", "POSTED", request.getAmount());
        }

        // Sender: DEBIT decreases liability/balance
        BigDecimal srcNewBal = applyEntryToBalance(srcAccount.getAccountType(), srcBalance.getCurrentBalance(),
                LedgerEntryDirection.DEBIT, request.getAmount());
        srcBalance.setCurrentBalance(srcNewBal);
        srcBalance.setAvailableBalance(srcNewBal);
        srcBalance.setLastTransactionId(savedTxn.getId());
        srcBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(srcBalance);

        LedgerEntry srcEntry = new LedgerEntry(
                generateEntryReference(txnRef, 1),
                srcAccount,
                LedgerEntryDirection.DEBIT,
                request.getAmount(),
                srcNewBal,
                1,
                "Transfer Debit to " + destAccount.getAccountNumber()
        );
        srcEntry.setTransaction(savedTxn);

        // Recipient: CREDIT increases liability/balance
        BigDecimal destNewBal = applyEntryToBalance(destAccount.getAccountType(), destBalance.getCurrentBalance(),
                LedgerEntryDirection.CREDIT, request.getAmount());
        destBalance.setCurrentBalance(destNewBal);
        destBalance.setAvailableBalance(destNewBal);
        destBalance.setLastTransactionId(savedTxn.getId());
        destBalance.setLastTransactionReference(savedTxn.getTransactionReference());
        accountBalanceRepository.save(destBalance);

        LedgerEntry destEntry = new LedgerEntry(
                generateEntryReference(txnRef, 2),
                destAccount,
                LedgerEntryDirection.CREDIT,
                request.getAmount(),
                destNewBal,
                2,
                "Transfer Credit from " + srcAccount.getAccountNumber()
        );
        destEntry.setTransaction(savedTxn);

        List<LedgerEntry> entries = List.of(srcEntry, destEntry);
        validateBalancedTransaction(entries);

        ledgerEntryRepository.saveAll(entries);
        savedTxn.setEntries(new ArrayList<>(entries));

        log.info("Transfer completed: txnRef={}, amount={}, from={}, to={}", txnRef, request.getAmount(),
                srcAccount.getAccountNumber(), destAccount.getAccountNumber());
        return new LedgerTransactionResponse(savedTxn);
    }

    /**
     * Reverse an existing transaction by creating equal and opposite compensating entries.
     * Immutable rule: Original posted entries are NEVER modified or deleted.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionReversalResponse reverseTransaction(Long transactionId, LedgerReversalRequest request, String initiatedBy) {
        // Idempotency check for reversal
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<LedgerTransaction> existingRevTxn = ledgerTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingRevTxn.isPresent()) {
                Optional<TransactionReversal> existingRev = transactionReversalRepository.findByOriginalTransactionId(transactionId);
                if (existingRev.isPresent()) {
                    return new TransactionReversalResponse(existingRev.get());
                }
            }
        }

        LedgerTransaction originalTxn = ledgerTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new LedgerTransactionNotFoundException("Transaction not found to reverse with id: " + transactionId));

        if (originalTxn.getTransactionType() == LedgerTransactionType.REVERSAL) {
            throw new IllegalLedgerOperationException("Cannot reverse a transaction of type REVERSAL: " + originalTxn.getTransactionReference());
        }
        if (originalTxn.getStatus() == LedgerTransactionStatus.REVERSED) {
            throw new IllegalLedgerOperationException("Transaction " + originalTxn.getTransactionReference() + " is already REVERSED");
        }
        if (originalTxn.getStatus() != LedgerTransactionStatus.POSTED) {
            throw new IllegalLedgerOperationException("Only POSTED transactions can be reversed. Current status: " + originalTxn.getStatus());
        }
        if (transactionReversalRepository.existsByOriginalTransactionId(transactionId)) {
            throw new IllegalLedgerOperationException("A reversal record already exists for transaction " + originalTxn.getTransactionReference());
        }

        List<LedgerEntry> originalEntries = ledgerEntryRepository.findByTransactionIdOrderByEntryOrderAsc(transactionId);
        if (originalEntries.isEmpty()) {
            throw new IllegalLedgerOperationException("Cannot reverse transaction with no posted entries: " + transactionId);
        }

        // Collect all distinct accounts and lock them in ascending order of ID
        List<Long> accountIds = originalEntries.stream()
                .map(e -> e.getAccount().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, AccountBalance> lockedBalances = new HashMap<>();
        for (Long accId : accountIds) {
            lockedBalances.put(accId, getBalanceWithLock(accId));
        }

        String revRef = generateTransactionReference("REV");
        String idempKey = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                : "REV-" + originalTxn.getTransactionReference() + "-" + UUID.randomUUID().toString().substring(0, 8);

        LedgerTransaction revTxn = new LedgerTransaction(
                revRef,
                idempKey,
                LedgerTransactionType.REVERSAL,
                originalTxn.getAmount(),
                originalTxn.getCurrency(),
                "Reversal of " + originalTxn.getTransactionReference() + ": " + request.getReason(),
                initiatedBy
        );
        LedgerTransaction savedRevTxn = ledgerTransactionRepository.save(revTxn);
        if (fintechMetricsService != null) {
            fintechMetricsService.recordLedgerTransaction("REVERSAL", "POSTED", revTxn.getAmount());
        }

        List<LedgerEntry> reversalEntries = new ArrayList<>();
        int order = 1;
        for (LedgerEntry origEntry : originalEntries) {
            LedgerAccount account = origEntry.getAccount();
            AccountBalance balance = lockedBalances.get(account.getId());

            // Equal and opposite direction
            LedgerEntryDirection oppositeDirection = (origEntry.getDirection() == LedgerEntryDirection.DEBIT)
                    ? LedgerEntryDirection.CREDIT
                    : LedgerEntryDirection.DEBIT;

            BigDecimal newBalance = applyEntryToBalance(account.getAccountType(), balance.getCurrentBalance(),
                    oppositeDirection, origEntry.getAmount());

            balance.setCurrentBalance(newBalance);
            balance.setAvailableBalance(newBalance);
            balance.setLastTransactionId(savedRevTxn.getId());
            balance.setLastTransactionReference(savedRevTxn.getTransactionReference());
            accountBalanceRepository.save(balance);

            LedgerEntry revEntry = new LedgerEntry(
                    generateEntryReference(revRef, order++),
                    account,
                    oppositeDirection,
                    origEntry.getAmount(),
                    newBalance,
                    order,
                    "Reversal offset for entry " + origEntry.getEntryReference()
            );
            revEntry.setTransaction(savedRevTxn);
            reversalEntries.add(revEntry);
        }

        validateBalancedTransaction(reversalEntries);
        ledgerEntryRepository.saveAll(reversalEntries);
        savedRevTxn.setEntries(reversalEntries);

        // Mark original transaction as REVERSED
        originalTxn.setStatus(LedgerTransactionStatus.REVERSED);
        ledgerTransactionRepository.save(originalTxn);

        // Record immutable TransactionReversal audit link
        TransactionReversal reversalRecord = new TransactionReversal(
                originalTxn,
                savedRevTxn,
                request.getReason(),
                initiatedBy
        );
        TransactionReversal savedReversal = transactionReversalRepository.save(reversalRecord);

        log.info("Transaction reversed successfully: origRef={}, revRef={}, reason={}",
                originalTxn.getTransactionReference(), revRef, request.getReason());

        return new TransactionReversalResponse(savedReversal);
    }

    /**
     * Retrieve transaction details including full double-entry ledger entries.
     */
    @Transactional(readOnly = true)
    public LedgerTransactionResponse getTransactionById(Long transactionId) {
        LedgerTransaction txn = ledgerTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new LedgerTransactionNotFoundException("Transaction not found with id: " + transactionId));
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionIdOrderByEntryOrderAsc(transactionId);
        txn.setEntries(entries);
        return new LedgerTransactionResponse(txn);
    }

    /**
     * Retrieve immutable ledger entries for an account by its ID (ledger statement).
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAccountEntries(Long accountId, Pageable pageable) {
        if (!ledgerAccountRepository.existsById(accountId)) {
            throw new LedgerAccountNotFoundException("Ledger account not found with id: " + accountId);
        }
        return ledgerEntryRepository.findByAccountIdOrderByIdDesc(accountId, pageable)
                .map(LedgerEntryResponse::new);
    }

    /**
     * Retrieve immutable ledger entries for an account by its Account Number (ledger statement).
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAccountEntriesByNumber(String accountNumber, Pageable pageable) {
        if (!ledgerAccountRepository.existsByAccountNumber(accountNumber)) {
            throw new LedgerAccountNotFoundException("Ledger account not found: " + accountNumber);
        }
        return ledgerEntryRepository.findByAccountAccountNumberOrderByIdDesc(accountNumber, pageable)
                .map(LedgerEntryResponse::new);
    }

    // --- Helper & Invariant Enforcement Methods ---

    /**
     * Enforces the fundamental accounting equation on the ledger entries.
     * Total Debit must strictly equal Total Credit (Sum(Debit) == Sum(Credit)).
     */
    public void validateBalancedTransaction(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new UnbalancedLedgerTransactionException("Ledger transaction must contain at least two entries");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new UnbalancedLedgerTransactionException("Ledger entry amount must be strictly greater than zero");
            }
            if (entry.getDirection() == LedgerEntryDirection.DEBIT) {
                totalDebit = totalDebit.add(entry.getAmount());
            } else if (entry.getDirection() == LedgerEntryDirection.CREDIT) {
                totalCredit = totalCredit.add(entry.getAmount());
            } else {
                throw new UnbalancedLedgerTransactionException("Invalid entry direction: " + entry.getDirection());
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedLedgerTransactionException(String.format(
                    "Double-entry violation! Total Debit (%s) does not equal Total Credit (%s)", totalDebit, totalCredit
            ));
        }
    }

    /**
     * Apply a debit or credit entry to an account balance respecting the account's normal balance.
     * - ASSET & EXPENSE: Normal Balance = DEBIT. (Debit +, Credit -)
     * - LIABILITY, EQUITY, REVENUE: Normal Balance = CREDIT. (Credit +, Debit -)
     */
    public BigDecimal applyEntryToBalance(LedgerAccountType accountType, BigDecimal currentBal,
                                          LedgerEntryDirection direction, BigDecimal amount) {
        BigDecimal bal = (currentBal != null) ? currentBal : BigDecimal.ZERO;
        boolean isNormalDebit = (accountType == LedgerAccountType.ASSET || accountType == LedgerAccountType.EXPENSE);

        if (isNormalDebit) {
            return (direction == LedgerEntryDirection.DEBIT) ? bal.add(amount) : bal.subtract(amount);
        } else {
            return (direction == LedgerEntryDirection.CREDIT) ? bal.add(amount) : bal.subtract(amount);
        }
    }

    private AccountBalance getBalanceWithLock(Long accountId) {
        return accountBalanceRepository.findByAccountIdWithLock(accountId)
                .orElseGet(() -> {
                    LedgerAccount acc = ledgerAccountRepository.findById(accountId)
                            .orElseThrow(() -> new LedgerAccountNotFoundException("Account not found: " + accountId));
                    AccountBalance newBal = new AccountBalance(acc);
                    return accountBalanceRepository.save(newBal);
                });
    }

    private String generateTransactionReference(String prefix) {
        return String.format("%s-%d-%s", prefix, LocalDateTime.now().getYear(), UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private String generateEntryReference(String txnRef, int order) {
        return String.format("ENT-%s-%02d", txnRef, order);
    }
}

