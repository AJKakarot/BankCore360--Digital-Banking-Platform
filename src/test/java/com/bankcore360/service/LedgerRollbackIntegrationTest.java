package com.bankcore360.service;

import com.bankcore360.dto.LedgerTransferRequest;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InsufficientLedgerFundsException;
import com.bankcore360.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerRollbackIntegrationTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private LedgerTransactionRepository ledgerTransactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private LedgerAccount senderAccount;
    private LedgerAccount receiverAccount;
    private AccountBalance senderBalance;
    private AccountBalance receiverBalance;

    @BeforeEach
    void setUp() {
        senderAccount = new LedgerAccount("ACC-CUST-100", "Sender Account",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        senderAccount.setId(10L);

        receiverAccount = new LedgerAccount("ACC-CUST-200", "Receiver Account",
                LedgerAccountType.LIABILITY, "INR", LedgerAccountStatus.ACTIVE, null, false);
        receiverAccount.setId(20L);

        senderBalance = new AccountBalance(senderAccount);
        senderBalance.setId(100L);
        senderBalance.setCurrentBalance(new BigDecimal("5000.00"));
        senderBalance.setAvailableBalance(new BigDecimal("5000.00"));

        receiverBalance = new AccountBalance(receiverAccount);
        receiverBalance.setId(200L);
        receiverBalance.setCurrentBalance(new BigDecimal("1000.00"));
        receiverBalance.setAvailableBalance(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Rollback on Insufficient Funds: sender and receiver balances remain unchanged")
    void shouldRollbackWhenInsufficientFunds() {
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEM-ROLLBACK-001")).thenReturn(Optional.empty());
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-100")).thenReturn(Optional.of(senderAccount));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-200")).thenReturn(Optional.of(receiverAccount));
        when(accountBalanceRepository.findByAccountIdWithLock(10L)).thenReturn(Optional.of(senderBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(20L)).thenReturn(Optional.of(receiverBalance));

        LedgerTransferRequest request = new LedgerTransferRequest(
                "ACC-CUST-100",
                "ACC-CUST-200",
                new BigDecimal("99999.00"),
                "IDEM-ROLLBACK-001",
                "Should fail due to insufficient balance"
        );

        assertThatThrownBy(() -> ledgerService.transfer(request, "SYSTEM"))
                .isInstanceOf(InsufficientLedgerFundsException.class);

        // Balances must remain pristine
        assertThat(senderBalance.getCurrentBalance()).isEqualByComparingTo("5000.00");
        assertThat(receiverBalance.getCurrentBalance()).isEqualByComparingTo("1000.00");

        // Verify zero entries or transactions were saved
        verify(ledgerTransactionRepository, never()).save(any(LedgerTransaction.class));
        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Rollback on Persistence Fault: Aborts entire transfer if transaction save fails")
    void shouldRollbackOnPersistenceFailure() {
        when(ledgerTransactionRepository.findByIdempotencyKey("IDEM-FAIL-002")).thenReturn(Optional.empty());
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-100")).thenReturn(Optional.of(senderAccount));
        when(ledgerAccountRepository.findByAccountNumber("ACC-CUST-200")).thenReturn(Optional.of(receiverAccount));
        when(accountBalanceRepository.findByAccountIdWithLock(10L)).thenReturn(Optional.of(senderBalance));
        when(accountBalanceRepository.findByAccountIdWithLock(20L)).thenReturn(Optional.of(receiverBalance));
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenThrow(new RuntimeException("DB Disk Full"));

        LedgerTransferRequest request = new LedgerTransferRequest(
                "ACC-CUST-100",
                "ACC-CUST-200",
                new BigDecimal("500.00"),
                "IDEM-FAIL-002",
                "Disk fault simulation"
        );

        assertThatThrownBy(() -> ledgerService.transfer(request, "SYSTEM"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Disk Full");

        verify(ledgerEntryRepository, never()).saveAll(any());
    }
}
