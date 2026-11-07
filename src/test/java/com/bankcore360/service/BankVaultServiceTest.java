package com.bankcore360.service;

import com.bankcore360.dto.InjectCapitalRequest;
import com.bankcore360.dto.VaultStatusResponse;
import com.bankcore360.entity.*;
import com.bankcore360.exception.InsufficientBankLiquidityException;
import com.bankcore360.repository.BankAccountRepository;
import com.bankcore360.repository.BankVaultRepository;
import com.bankcore360.repository.VaultTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BankVaultServiceTest {

    @Mock
    private BankVaultRepository bankVaultRepository;

    @Mock
    private VaultTransactionRepository vaultTransactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankVaultService bankVaultService;

    private BankVault vault;
    private Customer customer;
    private LoanApplication application;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        vault = new BankVault(
                BankVault.DEFAULT_VAULT_ID,
                "BankCore360 Central Treasury & Reserve",
                new BigDecimal("10000000.00") // 100 Lakh
        );

        customer = new Customer();
        customer.setId(101L);
        customer.setFullName("Ajeet Sharma");
        customer.setEmail("ajeet@example.com");
        customer.setKycStatus(KycStatus.VERIFIED);

        application = new LoanApplication(
                "LN-2026-TEST01",
                customer,
                LoanType.PERSONAL,
                new BigDecimal("250000.00"),
                24,
                "Home Renovation"
        );
        application.setApprovedAmount(new BigDecimal("250000.00"));

        account = new BankAccount(
                "360100000101",
                customer,
                AccountType.SAVINGS,
                BigDecimal.ZERO,
                AccountStatus.ACTIVE
        );
    }

    @Test
    void testInitVault_WhenNotExists_ShouldCreateInitial100Lakh() {
        when(bankVaultRepository.existsById(BankVault.DEFAULT_VAULT_ID)).thenReturn(false);

        bankVaultService.initVault();

        verify(bankVaultRepository).save(any(BankVault.class));
        verify(vaultTransactionRepository).save(any(VaultTransaction.class));
    }

    @Test
    void testDisburseLoanFromVault_Success() {
        when(vaultTransactionRepository.findByIdempotencyKey("DISBURSE-LN-2026-TEST01")).thenReturn(Optional.empty());
        when(bankVaultRepository.findByIdWithLock(BankVault.DEFAULT_VAULT_ID)).thenReturn(Optional.of(vault));
        when(bankAccountRepository.findByCustomerIdOrderByCreatedAtDesc(101L)).thenReturn(List.of(account));
        when(vaultTransactionRepository.save(any(VaultTransaction.class))).thenAnswer(i -> i.getArgument(0));

        VaultTransaction txn = bankVaultService.disburseLoanFromVault(application, "manager_credit");

        assertNotNull(txn);
        assertEquals("DEBIT", txn.getDirection());
        assertEquals(new BigDecimal("250000.00"), txn.getAmount());
        assertEquals("DISBURSE-LN-2026-TEST01", txn.getIdempotencyKey());
        assertEquals(new BigDecimal("9750000.00"), vault.getAvailableLiquidity());
        assertEquals(new BigDecimal("250000.00"), account.getBalance());

        verify(bankVaultRepository).save(vault);
        verify(bankAccountRepository).save(account);
    }

    @Test
    void testDisburseLoanFromVault_Idempotency_ShouldNotDoubleDebit() {
        VaultTransaction existing = new VaultTransaction(
                "TXN-DSB-EXISTING",
                "DISBURSE-LN-2026-TEST01",
                VaultTransactionType.LOAN_DISBURSAL,
                "DEBIT",
                new BigDecimal("250000.00"),
                new BigDecimal("10000000.00"),
                new BigDecimal("9750000.00"),
                "LN-2026-TEST01",
                "360100000101",
                "Ajeet Sharma",
                "manager_credit",
                "Already disbursed"
        );
        when(vaultTransactionRepository.findByIdempotencyKey("DISBURSE-LN-2026-TEST01")).thenReturn(Optional.of(existing));

        VaultTransaction txn = bankVaultService.disburseLoanFromVault(application, "manager_credit");

        assertSame(existing, txn);
        verify(bankVaultRepository, never()).findByIdWithLock(any());
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void testDisburseLoanFromVault_InsufficientLiquidity_ShouldThrowException() {
        vault.setAvailableLiquidity(new BigDecimal("100000.00")); // Only 1 Lakh available
        when(vaultTransactionRepository.findByIdempotencyKey("DISBURSE-LN-2026-TEST01")).thenReturn(Optional.empty());
        when(bankVaultRepository.findByIdWithLock(BankVault.DEFAULT_VAULT_ID)).thenReturn(Optional.of(vault));

        assertThrows(InsufficientBankLiquidityException.class, () ->
                bankVaultService.disburseLoanFromVault(application, "manager_credit")
        );

        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void testInjectCapital_Success() {
        when(bankVaultRepository.findByIdWithLock(BankVault.DEFAULT_VAULT_ID)).thenReturn(Optional.of(vault));
        when(vaultTransactionRepository.save(any(VaultTransaction.class))).thenAnswer(i -> i.getArgument(0));

        InjectCapitalRequest req = new InjectCapitalRequest(new BigDecimal("5000000.00"), "Q1 Reserve Inflow");
        VaultTransaction txn = bankVaultService.injectCapital(req, "admin_user");

        assertNotNull(txn);
        assertEquals("CREDIT", txn.getDirection());
        assertEquals(new BigDecimal("5000000.00"), txn.getAmount());
        assertEquals(new BigDecimal("15000000.00"), vault.getAvailableLiquidity());
        assertEquals(new BigDecimal("15000000.00"), vault.getTotalCapital());

        verify(bankVaultRepository).save(vault);
    }

    @Test
    void testGetVaultStatus_CalculatesUtilization() {
        vault.setTotalDisbursed(new BigDecimal("2000000.00")); // 20 Lakh disbursed out of 100 Lakh
        when(bankVaultRepository.findById(BankVault.DEFAULT_VAULT_ID)).thenReturn(Optional.of(vault));
        when(vaultTransactionRepository.findTop20ByOrderByTimestampDesc()).thenReturn(List.of());

        VaultStatusResponse resp = bankVaultService.getVaultStatus();

        assertEquals(new BigDecimal("10000000.00"), resp.getTotalCapital());
        assertEquals(20.0, resp.getUtilizationPercent());
    }
}
