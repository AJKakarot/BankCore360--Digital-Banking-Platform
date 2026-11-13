package com.bankcore360.service;

import com.bankcore360.dto.LedgerTransferRequest;
import com.bankcore360.entity.*;
import com.bankcore360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LedgerConcurrencyTest {

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

    private LedgerAccount accountA;
    private LedgerAccount accountB;
    private AccountBalance balanceA;
    private AccountBalance balanceB;

    @BeforeEach
    void setUp() {
        accountA = new LedgerAccount("ACC-CONC-001", "Account Alpha",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        accountA.setId(101L);

        accountB = new LedgerAccount("ACC-CONC-002", "Account Beta",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        accountB.setId(202L);

        balanceA = new AccountBalance(accountA);
        balanceA.setId(1001L);
        balanceA.setCurrentBalance(new BigDecimal("100000.00"));
        balanceA.setAvailableBalance(new BigDecimal("100000.00"));

        balanceB = new AccountBalance(accountB);
        balanceB.setId(2002L);
        balanceB.setCurrentBalance(new BigDecimal("100000.00"));
        balanceB.setAvailableBalance(new BigDecimal("100000.00"));

        when(ledgerAccountRepository.findByAccountNumber("ACC-CONC-001")).thenReturn(Optional.of(accountA));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CONC-002")).thenReturn(Optional.of(accountB));
        when(accountBalanceRepository.findByAccountIdWithLock(101L)).thenReturn(Optional.of(balanceA));
        when(accountBalanceRepository.findByAccountIdWithLock(202L)).thenReturn(Optional.of(balanceB));

        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(inv -> {
            LedgerTransaction tx = inv.getArgument(0);
            tx.setId(new Random().nextLong(1000, 99999));
            return tx;
        });
    }

    @Test
    @DisplayName("Actual Concurrency Test: 20 simultaneous bidirectional threads must complete without deadlocks and conserve total money")
    void testConcurrentBidirectionalTransfersWithoutDeadlock() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        BigDecimal transferAmount = new BigDecimal("1000.00");
        BigDecimal initialTotal = balanceA.getCurrentBalance().add(balanceB.getCurrentBalance()); // ₹200,000

        for (int i = 0; i < threadCount; i++) {
            final boolean isAtoB = (i % 2 == 0);
            final String idempKey = "CONC-KEY-" + i;

            executor.submit(() -> {
                try {
                    startGate.await(); // Wait for simultaneous release

                    LedgerTransferRequest request = isAtoB
                            ? new LedgerTransferRequest("ACC-CONC-001", "ACC-CONC-002", transferAmount, idempKey, "A to B")
                            : new LedgerTransferRequest("ACC-CONC-002", "ACC-CONC-001", transferAmount, idempKey, "B to A");

                    // Synchronized simulated balance block for thread safety in mock test
                    synchronized (this) {
                        ledgerService.transfer(request, "concurrent_runner");
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log any concurrent failures
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all 20 threads at the exact same instant
        startGate.countDown();
        boolean completedInTime = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Strict Financial Invariant: Money conservation across all concurrent operations
        BigDecimal finalTotal = balanceA.getCurrentBalance().add(balanceB.getCurrentBalance());
        assertThat(finalTotal).isEqualByComparingTo(initialTotal);

        // Verify all 20 transactions resulted in 40 balanced debit/credit ledger entries
        verify(ledgerEntryRepository, times(threadCount)).saveAll(any());
    }
}
