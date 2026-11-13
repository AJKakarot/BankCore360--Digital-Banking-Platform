package com.bankcore360.service;

import com.bankcore360.dto.DiscrepancyItemDto;
import com.bankcore360.dto.ReconciliationBatchReport;
import com.bankcore360.entity.*;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReconciliationEngineTest {

    @Mock
    private BankStatementRepository bankStatementRepository;

    @Mock
    private BankStatementItemRepository bankStatementItemRepository;

    @Mock
    private ReconciliationBatchRepository reconciliationBatchRepository;

    @Mock
    private ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private FintechMetricsService fintechMetricsService;

    private ReconciliationEngineService reconciliationEngineService;

    private BankStatement testStatement;
    private LedgerAccount testAccount;

    @BeforeEach
    void setUp() {
        reconciliationEngineService = new ReconciliationEngineService(
                bankStatementRepository,
                bankStatementItemRepository,
                reconciliationBatchRepository,
                reconciliationDiscrepancyRepository,
                ledgerEntryRepository,
                ledgerAccountRepository,
                fintechMetricsService
        );

        testStatement = new BankStatement("STMT-2026-001", "HDFC Bank", "360100000001", "stmt.csv", "hash123", "ADMIN");
        testStatement.setId(1L);
        testStatement.setStatementStartDate(LocalDate.of(2026, 11, 1));
        testStatement.setStatementEndDate(LocalDate.of(2026, 11, 30));

        testAccount = new LedgerAccount("360100000001", "Test Account", LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        testAccount.setId(10L);
    }

    @Test
    @DisplayName("Should successfully match exact reference and amount transactions")
    void testExactMatchReconciliation() {
        BankStatementItem item1 = new BankStatementItem("SITM-1", LocalDate.of(2026, 11, 2),
                new BigDecimal("5000.00"), LedgerEntryDirection.CREDIT, "Salary Credit", "TXN-REF-100");
        item1.setId(1L);

        when(bankStatementRepository.findById(1L)).thenReturn(Optional.of(testStatement));
        when(bankStatementItemRepository.findByStatementId(1L)).thenReturn(List.of(item1));
        when(ledgerAccountRepository.findByAccountNumber("360100000001")).thenReturn(Optional.of(testAccount));

        LedgerTransaction txn = new LedgerTransaction("TXN-REF-100", "IDEM-1", LedgerTransactionType.DEPOSIT,
                new BigDecimal("5000.00"), "INR", "Salary Credit", "USER1");
        txn.setId(50L);

        LedgerEntry entry = new LedgerEntry("ENT-001", testAccount, LedgerEntryDirection.CREDIT,
                new BigDecimal("5000.00"), new BigDecimal("15000.00"), 1, "Salary Credit");
        entry.setId(200L);
        entry.setTransaction(txn);

        when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(entry));
        when(reconciliationBatchRepository.save(any(ReconciliationBatch.class))).thenAnswer(i -> {
            ReconciliationBatch b = i.getArgument(0);
            b.setId(1001L);
            return b;
        });

        ReconciliationBatchReport report = reconciliationEngineService.executeReconciliation(1L, "ADMIN");

        assertNotNull(report);
        assertEquals(1, report.getMatchedCount());
        assertEquals(0, report.getDiscrepancyCount());
        assertEquals(100.0, report.getMatchRatePercentage());
        assertEquals(ReconciliationItemStatus.MATCHED, item1.getReconciliationStatus());
    }

    @Test
    @DisplayName("Should flag AMOUNT_MISMATCH discrepancy when reference matches but amount differs")
    void testAmountMismatchDiscrepancy() {
        BankStatementItem item = new BankStatementItem("SITM-2", LocalDate.of(2026, 11, 5),
                new BigDecimal("10000.00"), LedgerEntryDirection.DEBIT, "Wire Transfer", "TXN-WIRE-999");
        item.setId(2L);

        when(bankStatementRepository.findById(1L)).thenReturn(Optional.of(testStatement));
        when(bankStatementItemRepository.findByStatementId(1L)).thenReturn(List.of(item));
        when(ledgerAccountRepository.findByAccountNumber("360100000001")).thenReturn(Optional.of(testAccount));

        LedgerTransaction txn = new LedgerTransaction("TXN-WIRE-999", "IDEM-2", LedgerTransactionType.TRANSFER,
                new BigDecimal("12000.00"), "INR", "Wire Transfer", "USER1");
        LedgerEntry entry = new LedgerEntry("ENT-002", testAccount, LedgerEntryDirection.DEBIT,
                new BigDecimal("12000.00"), new BigDecimal("3000.00"), 1, "Wire Transfer");
        entry.setId(201L);
        entry.setTransaction(txn);

        when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(entry));
        when(reconciliationBatchRepository.save(any(ReconciliationBatch.class))).thenAnswer(i -> {
            ReconciliationBatch b = i.getArgument(0);
            b.setId(1002L);
            return b;
        });

        ReconciliationBatchReport report = reconciliationEngineService.executeReconciliation(1L, "ADMIN");

        assertNotNull(report);
        assertEquals(0, report.getMatchedCount());
        assertEquals(1, report.getDiscrepancyCount());
        assertEquals(DiscrepancyType.AMOUNT_MISMATCH, report.getDiscrepancies().get(0).getDiscrepancyType());
        assertEquals(new BigDecimal("-2000.00"), report.getDiscrepancies().get(0).getVariance());
    }

    @Test
    @DisplayName("Should flag MISSING_IN_LEDGER when statement transaction is unknown")
    void testMissingInLedgerDiscrepancy() {
        BankStatementItem item = new BankStatementItem("SITM-3", LocalDate.of(2026, 11, 7),
                new BigDecimal("3500.00"), LedgerEntryDirection.DEBIT, "Unknown Fee", "UNKNOWN-EXT-1");
        item.setId(3L);

        when(bankStatementRepository.findById(1L)).thenReturn(Optional.of(testStatement));
        when(bankStatementItemRepository.findByStatementId(1L)).thenReturn(List.of(item));
        when(ledgerAccountRepository.findByAccountNumber("360100000001")).thenReturn(Optional.of(testAccount));
        when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtAsc(10L)).thenReturn(Collections.emptyList());
        when(reconciliationBatchRepository.save(any(ReconciliationBatch.class))).thenAnswer(i -> {
            ReconciliationBatch b = i.getArgument(0);
            b.setId(1003L);
            return b;
        });

        ReconciliationBatchReport report = reconciliationEngineService.executeReconciliation(1L, "ADMIN");

        assertNotNull(report);
        assertEquals(1, report.getDiscrepancyCount());
        assertEquals(DiscrepancyType.MISSING_IN_LEDGER, report.getDiscrepancies().get(0).getDiscrepancyType());
    }

    @Test
    @DisplayName("Should resolve discrepancy with audit notes without modifying posted ledger records")
    void testResolveDiscrepancy() {
        ReconciliationDiscrepancy disc = new ReconciliationDiscrepancy(
                new ReconciliationBatch("BATCH-1", testStatement, "ADMIN"),
                null, 201L, "TXN-001", DiscrepancyType.AMOUNT_MISMATCH, "Mismatch",
                new BigDecimal("100.00"), new BigDecimal("120.00")
        );
        disc.setId(555L);

        when(reconciliationDiscrepancyRepository.findById(555L)).thenReturn(Optional.of(disc));
        when(reconciliationDiscrepancyRepository.save(any(ReconciliationDiscrepancy.class))).thenAnswer(i -> i.getArgument(0));

        DiscrepancyItemDto resolved = reconciliationEngineService.resolveDiscrepancy(
                555L, DiscrepancyStatus.RESOLVED, "Bank cleared the 20 INR variance as service charge", "CHIEF_AUDITOR"
        );

        assertNotNull(resolved);
        assertEquals(DiscrepancyStatus.RESOLVED, resolved.getStatus());
        assertEquals("CHIEF_AUDITOR", resolved.getResolvedBy());
        assertTrue(resolved.getInvestigatorNotes().contains("Bank cleared"));
        verify(ledgerEntryRepository, never()).save(any());
    }
}
