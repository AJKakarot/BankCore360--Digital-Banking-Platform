package com.bankcore360.service;

import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.exception.*;
import com.bankcore360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LedgerServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private LedgerTransactionRepository ledgerTransactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private TransactionReversalRepository transactionReversalRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private LedgerAccount vaultAccount;
    private AccountBalance vaultBalance;
    private LedgerAccount custAccountA;
    private AccountBalance custBalanceA;
    private LedgerAccount custAccountB;
    private AccountBalance custBalanceB;

    @BeforeEach
    void setUp() {
        vaultAccount = new LedgerAccount("SYS-VAULT-001", "BANK_CENTRAL_VAULT",
                LedgerAccountType.ASSET, "INR", LedgerAccountStatus.ACTIVE, null, true);
        vaultAccount.setId(1L);

        vaultBalance = new AccountBalance(vaultAccount);
        vaultBalance.setId(10L);
        vaultBalance.setCurrentBalance(new BigDecimal("10000000.00"));
        vaultBalance.setAvailableBalance(new BigDecimal("10000000.00"));

        custAccountA = new LedgerAccount("ACC-CUST-1001", "Customer A Savings",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        custAccountA.setId(2L);

        custBalanceA = new AccountBalance(custAccountA);
        custBalanceA.setId(20L);
        custBalanceA.setCurrentBalance(new BigDecimal("50000.00"));
        custBalanceA.setAvailableBalance(new BigDecimal("50000.00"));

        custAccountB = new LedgerAccount("ACC-CUST-1002", "Customer B Savings",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        custAccountB.setId(3L);

        custBalanceB = new AccountBalance(custAccountB);
        custBalanceB.setId(30L);
        custBalanceB.setCurrentBalance(new BigDecimal("10000.00"));
        custBalanceB.setAvailableBalance(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Should successfully execute deposit with balanced double-entry (Debit Vault, Credit Customer)")
    void testDepositSuccess() {
        when(ledgerAccountRepository.findByAccountNumber("SYS-VAULT-001")).thenReturn(Optional.of(vaultAccount));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-1001")).thenReturn(Optional.of(custAccountA));
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEMP-DEP-01")).thenReturn(Optional.empty());

        when(accountBalanceRepository.findByAccountIdWithLock(1L)).thenReturn(Optional.of(vaultBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(2L)).thenReturn(Optional.of(custBalanceA));
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        LedgerDepositRequest request = new LedgerDepositRequest(
                "ACC-CUST-1001",
                new BigDecimal("25000.00"),
                "IDEMP-DEP-01",
                "Customer initial deposit"
        );

        LedgerTransactionResponse response = ledgerService.deposit(request, "dev_customer");

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("25000.00");
        assertThat(response.getTransactionType()).isEqualTo(LedgerTransactionType.DEPOSIT);

        // Vault ASSET increased (Debit)
        assertThat(vaultBalance.getCurrentBalance()).isEqualByComparingTo("10025000.00");
        // Customer LIABILITY increased (Credit)
        assertThat(custBalanceA.getCurrentBalance()).isEqualByComparingTo("75000.00");

        verify(ledgerEntryRepository).saveAll(argThat(entries -> {
            List<LedgerEntry> list = (List<LedgerEntry>) entries;
            assertThat(list).hasSize(2);
            BigDecimal totalDebit = list.stream().filter(e -> e.getDirection() == LedgerEntryDirection.DEBIT)
                    .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = list.stream().filter(e -> e.getDirection() == LedgerEntryDirection.CREDIT)
                    .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return totalDebit.compareTo(totalCredit) == 0;
        }));
    }

    @Test
    @DisplayName("Should return existing transaction when deposit idempotency key is repeated")
    void testDepositIdempotency() {
        LedgerTransaction existing = new LedgerTransaction("TXN-DEP-001", "IDEMP-KEY-EXISTING",
                LedgerTransactionType.DEPOSIT, new BigDecimal("1000.00"), "INR", "Existing deposit", "user");
        existing.setId(99L);
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEMP-KEY-EXISTING")).thenReturn(Optional.of(existing));

        LedgerDepositRequest req = new LedgerDepositRequest("ACC-CUST-1001", new BigDecimal("1000.00"), "IDEMP-KEY-EXISTING", "retry");
        LedgerTransactionResponse res = ledgerService.deposit(req, "user");

        assertThat(res.getId()).isEqualTo(99L);
        assertThat(res.getTransactionReference()).isEqualTo("TXN-DEP-001");
        verifyNoInteractions(accountBalanceRepository);
    }

    @Test
    @DisplayName("Should successfully execute withdrawal with balanced double-entry (Debit Customer, Credit Vault)")
    void testWithdrawalSuccess() {
        when(ledgerAccountRepository.findByAccountNumber("SYS-VAULT-001")).thenReturn(Optional.of(vaultAccount));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-1001")).thenReturn(Optional.of(custAccountA));
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEMP-WTD-01")).thenReturn(Optional.empty());

        when(accountBalanceRepository.findByAccountIdWithLock(1L)).thenReturn(Optional.of(vaultBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(2L)).thenReturn(Optional.of(custBalanceA));
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction tx = invocation.getArgument(0);
            tx.setId(101L);
            return tx;
        });

        LedgerWithdrawRequest request = new LedgerWithdrawRequest(
                "ACC-CUST-1001",
                new BigDecimal("20000.00"),
                "IDEMP-WTD-01",
                "ATM cash withdrawal"
        );

        LedgerTransactionResponse response = ledgerService.withdraw(request, "dev_customer");

        assertThat(response).isNotNull();
        // Customer balance reduced
        assertThat(custBalanceA.getCurrentBalance()).isEqualByComparingTo("30000.00");
        // Vault balance reduced
        assertThat(vaultBalance.getCurrentBalance()).isEqualByComparingTo("9980000.00");
    }

    @Test
    @DisplayName("Should reject withdrawal if customer account has insufficient funds")
    void testWithdrawalInsufficientFunds() {
        when(ledgerAccountRepository.findByAccountNumber("SYS-VAULT-001")).thenReturn(Optional.of(vaultAccount));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-1001")).thenReturn(Optional.of(custAccountA));
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEMP-WTD-FAIL")).thenReturn(Optional.empty());

        when(accountBalanceRepository.findByAccountIdWithLock(1L)).thenReturn(Optional.of(vaultBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(2L)).thenReturn(Optional.of(custBalanceA));

        LedgerWithdrawRequest request = new LedgerWithdrawRequest(
                "ACC-CUST-1001",
                new BigDecimal("60000.00"), // available is 50000
                "IDEMP-WTD-FAIL",
                "Excessive withdrawal"
        );

        assertThatThrownBy(() -> ledgerService.withdraw(request, "dev_customer"))
                .isInstanceOf(InsufficientLedgerFundsException.class)
                .hasMessageContaining("Insufficient funds in account ACC-CUST-1001");
    }

    @Test
    @DisplayName("Should successfully transfer funds between two customer accounts (Debit Sender, Credit Recipient)")
    void testTransferSuccess() {
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-1001")).thenReturn(Optional.of(custAccountA));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-1002")).thenReturn(Optional.of(custAccountB));
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEMP-TRF-01")).thenReturn(Optional.empty());

        when(accountBalanceRepository.findByAccountIdWithLock(2L)).thenReturn(Optional.of(custBalanceA));
        when(accountBalanceRepository.findByAccountIdWithLock(3L)).thenReturn(Optional.of(custBalanceB));
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction tx = invocation.getArgument(0);
            tx.setId(102L);
            return tx;
        });

        LedgerTransferRequest request = new LedgerTransferRequest(
                "ACC-CUST-1001",
                "ACC-CUST-1002",
                new BigDecimal("15000.00"),
                "IDEMP-TRF-01",
                "P2P payment"
        );

        LedgerTransactionResponse response = ledgerService.transfer(request, "dev_customer");

        assertThat(response).isNotNull();
        // Sender decreased: 50,000 - 15,000 = 35,000
        assertThat(custBalanceA.getCurrentBalance()).isEqualByComparingTo("35000.00");
        // Receiver increased: 10,000 + 15,000 = 25,000
        assertThat(custBalanceB.getCurrentBalance()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("Should prevent self-transfer when source and destination accounts are identical")
    void testTransferIdenticalAccounts() {
        LedgerTransferRequest request = new LedgerTransferRequest(
                "ACC-CUST-1001",
                "ACC-CUST-1001",
                new BigDecimal("5000.00"),
                "IDEMP-SELF",
                "Self transfer"
        );

        assertThatThrownBy(() -> ledgerService.transfer(request, "user"))
                .isInstanceOf(IllegalLedgerOperationException.class)
                .hasMessageContaining("Source and destination accounts cannot be identical");
    }

    @Test
    @DisplayName("Should reverse transaction with equal and opposite compensating entries without deleting historical records")
    void testReverseTransactionSuccess() {
        LedgerTransaction origTxn = new LedgerTransaction("TXN-DEP-555", "ORIG-IDEMP",
                LedgerTransactionType.DEPOSIT, new BigDecimal("10000.00"), "INR", "Deposit", "user");
        origTxn.setId(555L);
        origTxn.setStatus(LedgerTransactionStatus.POSTED);

        LedgerEntry entry1 = new LedgerEntry("ENT-01", vaultAccount, LedgerEntryDirection.DEBIT,
                new BigDecimal("10000.00"), new BigDecimal("10010000.00"), 1, "Vault debit");
        LedgerEntry entry2 = new LedgerEntry("ENT-02", custAccountA, LedgerEntryDirection.CREDIT,
                new BigDecimal("10000.00"), new BigDecimal("60000.00"), 2, "Cust credit");
        origTxn.setEntries(List.of(entry1, entry2));

        when(ledgerTransactionRepository.findById(555L)).thenReturn(Optional.of(origTxn));
        when(transactionReversalRepository.existsByOriginalTransactionId(555L)).thenReturn(false);
        when(ledgerEntryRepository.findByTransactionIdOrderByEntryOrderAsc(555L)).thenReturn(List.of(entry1, entry2));

        when(accountBalanceRepository.findByAccountIdWithLock(1L)).thenReturn(Optional.of(vaultBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(2L)).thenReturn(Optional.of(custBalanceA));

        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) tx.setId(600L);
            return tx;
        });

        when(transactionReversalRepository.save(any(TransactionReversal.class))).thenAnswer(invocation -> {
            TransactionReversal rev = invocation.getArgument(0);
            rev.setId(901L);
            return rev;
        });

        LedgerReversalRequest revReq = new LedgerReversalRequest("Erroneous deposit", "REV-IDEMP-01");
        TransactionReversalResponse revResponse = ledgerService.reverseTransaction(555L, revReq, "admin_user");

        assertThat(revResponse).isNotNull();
        assertThat(origTxn.getStatus()).isEqualTo(LedgerTransactionStatus.REVERSED);

        // Vault ASSET was credited (decreased by 10,000)
        assertThat(vaultBalance.getCurrentBalance()).isEqualByComparingTo("9990000.00");
        // Customer LIABILITY was debited (decreased by 10,000)
        assertThat(custBalanceA.getCurrentBalance()).isEqualByComparingTo("40000.00");

        verify(transactionReversalRepository).save(any(TransactionReversal.class));
    }

    @Test
    @DisplayName("Should prevent duplicate reversal when transaction is already REVERSED")
    void testCannotReverseAlreadyReversedTransaction() {
        LedgerTransaction txn = new LedgerTransaction("TXN-1", "K1", LedgerTransactionType.DEPOSIT,
                new BigDecimal("1000.00"), "INR", "desc", "user");
        txn.setId(77L);
        txn.setStatus(LedgerTransactionStatus.REVERSED);

        when(ledgerTransactionRepository.findById(77L)).thenReturn(Optional.of(txn));

        LedgerReversalRequest req = new LedgerReversalRequest("Duplicate reversal test", "REV-K-DUP");

        assertThatThrownBy(() -> ledgerService.reverseTransaction(77L, req, "admin"))
                .isInstanceOf(IllegalLedgerOperationException.class)
                .hasMessageContaining("already REVERSED");
    }

    @Test
    @DisplayName("Should strictly prevent reversing a transaction of type REVERSAL")
    void testCannotReverseReversalTransactionType() {
        LedgerTransaction revTxn = new LedgerTransaction("REV-1", "K2", LedgerTransactionType.REVERSAL,
                new BigDecimal("1000.00"), "INR", "reversal", "user");
        revTxn.setId(88L);
        revTxn.setStatus(LedgerTransactionStatus.POSTED);

        when(ledgerTransactionRepository.findById(88L)).thenReturn(Optional.of(revTxn));

        LedgerReversalRequest req = new LedgerReversalRequest("Attempt reverse reversal", "REV-K-3");

        assertThatThrownBy(() -> ledgerService.reverseTransaction(88L, req, "admin"))
                .isInstanceOf(IllegalLedgerOperationException.class)
                .hasMessageContaining("Cannot reverse a transaction of type REVERSAL");
    }

    @Test
    @DisplayName("Should throw UnbalancedLedgerTransactionException when sum of Debits != sum of Credits")
    void testValidateUnbalancedTransactionThrows() {
        LedgerEntry entry1 = new LedgerEntry("E1", vaultAccount, LedgerEntryDirection.DEBIT,
                new BigDecimal("1000.00"), BigDecimal.ZERO, 1, "test");
        LedgerEntry entry2 = new LedgerEntry("E2", custAccountA, LedgerEntryDirection.CREDIT,
                new BigDecimal("950.00"), BigDecimal.ZERO, 2, "test");

        assertThatThrownBy(() -> ledgerService.validateBalancedTransaction(List.of(entry1, entry2)))
                .isInstanceOf(UnbalancedLedgerTransactionException.class)
                .hasMessageContaining("Double-entry violation! Total Debit (1000.00) does not equal Total Credit (950.00)");
    }
}

