package com.bankcore360.service;

import com.bankcore360.dto.internal.*;
import com.bankcore360.entity.*;
import com.bankcore360.exception.AccountNotFoundException;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InternalAiBankingService {

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public InternalAiBankingService(
            AppUserRepository appUserRepository,
            CustomerRepository customerRepository,
            BankAccountRepository bankAccountRepository,
            LedgerAccountRepository ledgerAccountRepository,
            AccountBalanceRepository accountBalanceRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerTransactionRepository ledgerTransactionRepository,
            FraudAssessmentRepository fraudAssessmentRepository,
            FraudAlertRepository fraudAlertRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.customerRepository = customerRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public Customer resolveCustomer(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Authenticated username cannot be empty");
        }
        AppUser user = appUserRepository.findByUsername(username)
                .or(() -> appUserRepository.findByEmail(username.toLowerCase()))
                .orElse(null);

        String email = user != null ? user.getEmail() : username;
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found for user: " + username));
    }

    public List<InternalCustomerAccountDto> getCustomerAccounts(String username) {
        Customer customer = resolveCustomer(username);
        List<InternalCustomerAccountDto> dtos = new ArrayList<>();

        // Check BankAccount
        List<BankAccount> bankAccounts = bankAccountRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        for (BankAccount acc : bankAccounts) {
            dtos.add(new InternalCustomerAccountDto(
                    acc.getAccountNumber(),
                    acc.getAccountType().name(),
                    acc.getBalance(),
                    "INR",
                    acc.getAccountStatus().name(),
                    acc.getCreatedAt()
            ));
        }

        // Check LedgerAccount
        List<LedgerAccount> ledgerAccounts = ledgerAccountRepository.findByCustomerId(customer.getId());
        for (LedgerAccount acc : ledgerAccounts) {
            if (dtos.stream().noneMatch(d -> d.getAccountNumber().equals(acc.getAccountNumber()))) {
                BigDecimal bal = accountBalanceRepository.findByAccountId(acc.getId())
                        .map(AccountBalance::getAvailableBalance)
                        .orElse(BigDecimal.ZERO);
                dtos.add(new InternalCustomerAccountDto(
                        acc.getAccountNumber(),
                        acc.getAccountType().name(),
                        bal,
                        acc.getCurrency(),
                        acc.getStatus().name(),
                        acc.getCreatedAt()
                ));
            }
        }

        return dtos;
    }

    public InternalSpendingSummaryDto getCustomerSpendingSummary(String username) {
        Customer customer = resolveCustomer(username);
        List<LedgerAccount> accounts = ledgerAccountRepository.findByCustomerId(customer.getId());
        List<BankAccount> bankAccounts = bankAccountRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());

        Set<Long> accountIds = accounts.stream().map(LedgerAccount::getId).collect(Collectors.toSet());
        Set<String> accountNumbers = new HashSet<>();
        accounts.forEach(a -> accountNumbers.add(a.getAccountNumber()));
        bankAccounts.forEach(a -> accountNumbers.add(a.getAccountNumber()));

        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalOutflow = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryMap = new LinkedHashMap<>();
        categoryMap.put("DEPOSITS", BigDecimal.ZERO);
        categoryMap.put("TRANSFERS_OUT", BigDecimal.ZERO);
        categoryMap.put("WITHDRAWALS", BigDecimal.ZERO);
        categoryMap.put("LOAN_DISBURSEMENT", BigDecimal.ZERO);
        categoryMap.put("OTHER", BigDecimal.ZERO);

        int totalTxnCount = 0;

        for (Long accId : accountIds) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByIdDesc(accId);
            for (LedgerEntry entry : entries) {
                totalTxnCount++;
                BigDecimal amt = entry.getAmount();
                LedgerTransaction txn = entry.getTransaction();
                String txnType = txn != null ? txn.getTransactionType().name() : "OTHER";

                if (entry.getDirection() == LedgerEntryDirection.CREDIT) {
                    // Customer Liability increase = money credited to customer
                    totalInflow = totalInflow.add(amt);
                    if (txnType.contains("DEPOSIT")) {
                        categoryMap.put("DEPOSITS", categoryMap.get("DEPOSITS").add(amt));
                    } else if (txnType.contains("DISBURSE")) {
                        categoryMap.put("LOAN_DISBURSEMENT", categoryMap.get("LOAN_DISBURSEMENT").add(amt));
                    }
                } else {
                    // Customer Liability decrease = money debited from customer
                    totalOutflow = totalOutflow.add(amt);
                    if (txnType.contains("WITHDRAW")) {
                        categoryMap.put("WITHDRAWALS", categoryMap.get("WITHDRAWALS").add(amt));
                    } else if (txnType.contains("TRANSFER")) {
                        categoryMap.put("TRANSFERS_OUT", categoryMap.get("TRANSFERS_OUT").add(amt));
                    } else {
                        categoryMap.put("OTHER", categoryMap.get("OTHER").add(amt));
                    }
                }
            }
        }

        BigDecimal netSavings = totalInflow.subtract(totalOutflow);

        // Determine highest spending category
        String highestCategory = "WITHDRAWALS";
        BigDecimal maxSpend = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : categoryMap.entrySet()) {
            if (!entry.getKey().equals("DEPOSITS") && !entry.getKey().equals("LOAN_DISBURSEMENT")) {
                if (entry.getValue().compareTo(maxSpend) > 0) {
                    maxSpend = entry.getValue();
                    highestCategory = entry.getKey();
                }
            }
        }

        String momComp = totalOutflow.compareTo(BigDecimal.ZERO) == 0
                ? "No outflows recorded in current cycle. Financial posture is fully conserved."
                : "Spending across categories is within healthy reserve parameters. Net savings rate: "
                + (totalInflow.compareTo(BigDecimal.ZERO) > 0
                ? netSavings.multiply(BigDecimal.valueOf(100)).divide(totalInflow, 1, RoundingMode.HALF_UP) + "%"
                : "0%");

        return new InternalSpendingSummaryDto(
                username,
                totalInflow.setScale(2, RoundingMode.HALF_UP),
                totalOutflow.setScale(2, RoundingMode.HALF_UP),
                netSavings.setScale(2, RoundingMode.HALF_UP),
                totalTxnCount,
                categoryMap,
                highestCategory,
                momComp
        );
    }

    public List<InternalTransactionDetailDto> getCustomerTransactions(String username) {
        Customer customer = resolveCustomer(username);
        List<LedgerAccount> accounts = ledgerAccountRepository.findByCustomerId(customer.getId());
        List<InternalTransactionDetailDto> results = new ArrayList<>();

        for (LedgerAccount acc : accounts) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByIdDesc(acc.getId());
            for (LedgerEntry entry : entries) {
                LedgerTransaction txn = entry.getTransaction();
                if (txn != null) {
                    boolean isDebit = entry.getDirection() == LedgerEntryDirection.DEBIT;
                    results.add(new InternalTransactionDetailDto(
                            txn.getId(),
                            txn.getTransactionReference(),
                            txn.getPostedAt(),
                            entry.getAmount(),
                            txn.getTransactionType().name(),
                            txn.getStatus().name(),
                            txn.getDescription() != null ? txn.getDescription() : "Ledger Transaction " + txn.getTransactionType().name(),
                            isDebit ? "DEBIT" : "CREDIT",
                            acc.getAccountNumber(),
                            txn.getStatus() == LedgerTransactionStatus.REVERSED,
                            null,
                            List.of("Entry Reference: " + entry.getEntryReference())
                    ));
                }
            }
        }

        return results.stream().limit(50).collect(Collectors.toList());
    }

    public InternalTransactionDetailDto getTransactionDetail(String username, String refOrId) {
        Customer customer = resolveCustomer(username);
        List<LedgerAccount> customerAccounts = ledgerAccountRepository.findByCustomerId(customer.getId());
        Set<Long> customerAccountIds = customerAccounts.stream().map(LedgerAccount::getId).collect(Collectors.toSet());

        LedgerTransaction txn = null;
        if (refOrId.matches("\\d+")) {
            txn = ledgerTransactionRepository.findById(Long.parseLong(refOrId)).orElse(null);
        }
        if (txn == null) {
            txn = ledgerTransactionRepository.findByTransactionReference(refOrId)
                    .orElseThrow(() -> new AccountNotFoundException("Transaction not found: " + refOrId));
        }

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionIdOrderByEntryOrderAsc(txn.getId());
        boolean customerOwnsTransaction = entries.stream().anyMatch(e -> customerAccountIds.contains(e.getAccount().getId()));

        if (!customerOwnsTransaction) {
            throw new AccessDeniedException("Access denied: You do not have ownership of transaction " + refOrId);
        }

        LedgerEntry customerEntry = entries.stream()
                .filter(e -> customerAccountIds.contains(e.getAccount().getId()))
                .findFirst()
                .orElse(entries.get(0));

        List<String> counterparties = entries.stream()
                .filter(e -> !customerAccountIds.contains(e.getAccount().getId()))
                .map(e -> e.getAccount().getAccountNumber() + " (" + e.getDirection() + ")")
                .collect(Collectors.toList());

        return new InternalTransactionDetailDto(
                txn.getId(),
                txn.getTransactionReference(),
                txn.getPostedAt(),
                customerEntry.getAmount(),
                txn.getTransactionType().name(),
                txn.getStatus().name(),
                txn.getDescription(),
                customerEntry.getDirection() == LedgerEntryDirection.DEBIT ? "DEBIT" : "CREDIT",
                customerEntry.getAccount().getAccountNumber(),
                txn.getStatus() == LedgerTransactionStatus.REVERSED,
                txn.getStatus() == LedgerTransactionStatus.REVERSED ? "Reversal executed via compensating ledger entry." : null,
                counterparties
        );
    }

    public InternalCustomerFraudMetadataDto getCustomerFraudMetadata(String username) {
        Customer customer = resolveCustomer(username);
        List<BankAccount> bankAccounts = bankAccountRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        List<LedgerAccount> ledgerAccounts = ledgerAccountRepository.findByCustomerId(customer.getId());

        Set<String> accountNumbers = new HashSet<>();
        bankAccounts.forEach(a -> accountNumbers.add(a.getAccountNumber()));
        ledgerAccounts.forEach(a -> accountNumbers.add(a.getAccountNumber()));

        List<InternalCustomerFraudMetadataDto.FraudAssessmentSummaryDto> assessmentSummaries = new ArrayList<>();
        int totalScore = 0;
        int count = 0;
        int activeAlertsCount = 0;

        for (String accNum : accountNumbers) {
            List<FraudAlert> alerts = fraudAlertRepository.findByAccountNumberOrderByCreatedAtDesc(accNum);
            activeAlertsCount += (int) alerts.stream()
                    .filter(a -> a.getStatus() == FraudAlertStatus.PENDING_REVIEW || a.getStatus() == FraudAlertStatus.UNDER_INVESTIGATION)
                    .count();

            org.springframework.data.domain.Page<FraudAssessment> assessments =
                    fraudAssessmentRepository.findByAccountNumberOrderByEvaluatedAtDesc(
                            accNum, org.springframework.data.domain.PageRequest.of(0, 10));

            for (FraudAssessment ass : assessments.getContent()) {
                totalScore += ass.getRiskScore();
                count++;
                List<String> rules = new ArrayList<>();
                if (ass.getTriggeredRuleCodes() != null && !ass.getTriggeredRuleCodes().isBlank()) {
                    rules.addAll(Arrays.asList(ass.getTriggeredRuleCodes().split(";\\s*")));
                }
                assessmentSummaries.add(new InternalCustomerFraudMetadataDto.FraudAssessmentSummaryDto(
                        ass.getAssessmentReference(),
                        ass.getAssessmentReference(), // transaction ref fallback
                        ass.getRiskScore(),
                        ass.getRiskLevel().name(),
                        ass.getDecision().name(),
                        rules,
                        ass.getEvaluatedAt()
                ));
            }
        }

        int avgScore = count > 0 ? (totalScore / count) : 10;
        String riskLevel = avgScore <= 30 ? "LOW" : avgScore <= 60 ? "MEDIUM" : avgScore <= 80 ? "HIGH" : "CRITICAL";

        return new InternalCustomerFraudMetadataDto(
                username,
                avgScore,
                riskLevel,
                assessmentSummaries.size(),
                activeAlertsCount,
                assessmentSummaries
        );
    }

    public List<InternalLoanProductDto> getLoanProducts() {
        return List.of(
                new InternalLoanProductDto("PERSONAL", "Personal Loan",
                        new BigDecimal("25000.00"), new BigDecimal("1000000.00"),
                        6, 60, new BigDecimal("11.50"), true,
                        "Unsecured retail financing for personal emergencies, education, travel or medical needs. Minimum monthly income: ₹15,000; Max FOIR: 50%."),
                new InternalLoanProductDto("HOME", "Home & Housing Finance",
                        new BigDecimal("500000.00"), new BigDecimal("15000000.00"),
                        12, 360, new BigDecimal("8.75"), true,
                        "Long-term housing loans for purchase, construction, or renovation of residential properties with tax benefits under Sec 80C & 24."),
                new InternalLoanProductDto("EDUCATION", "Higher Education Loan",
                        new BigDecimal("50000.00"), new BigDecimal("3000000.00"),
                        12, 180, new BigDecimal("9.25"), true,
                        "Financing for higher studies in India and abroad covering tuition, hostel, and study material. Moratorium: Course duration + 6 months."),
                new InternalLoanProductDto("BUSINESS", "Commercial & MSME Loan",
                        new BigDecimal("100000.00"), new BigDecimal("5000000.00"),
                        12, 84, new BigDecimal("12.00"), true,
                        "Working capital and equipment financing for registered business entities and growth enterprises.")
        );
    }
}
