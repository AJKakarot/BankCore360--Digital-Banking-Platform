package com.bankcore360.service;

import com.bankcore360.dto.BankAccountResponse;
import com.bankcore360.dto.CreateAccountRequest;
import com.bankcore360.entity.*;
import com.bankcore360.exception.AccountNotFoundException;
import com.bankcore360.exception.DuplicateAccountException;
import com.bankcore360.exception.InvalidAccountStateException;
import com.bankcore360.repository.AccountAuditLogRepository;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.BankAccountRepository;
import com.bankcore360.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private AccountAuditLogRepository accountAuditLogRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private BankAccountService bankAccountService;

    private Customer sampleCustomer;
    private Customer anotherCustomer;
    private AppUser sampleUser;
    private Authentication customerAuth;
    private Authentication managerAuth;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setFullName("John Doe");
        sampleCustomer.setEmail("john.doe@bankcore360.com");
        sampleCustomer.setKycStatus(KycStatus.VERIFIED);

        sampleUser = new AppUser("john.doe", "john.doe@bankcore360.com", "password", UserRole.CUSTOMER);

        anotherCustomer = new Customer();
        anotherCustomer.setId(2L);
        anotherCustomer.setFullName("Jane Smith");
        anotherCustomer.setEmail("jane.smith@bankcore360.com");
        anotherCustomer.setKycStatus(KycStatus.VERIFIED);

        customerAuth = new TestingAuthenticationToken("john.doe", null, "ROLE_CUSTOMER");
        managerAuth = new TestingAuthenticationToken("dev_manager", null, "ROLE_MANAGER");
    }

    @Test
    @DisplayName("Should create SAVINGS account with ACTIVE status and ZERO balance for verified KYC customer")
    void shouldCreateSavingsAccountForVerifiedCustomer() {
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(sampleUser));
        when(customerRepository.findByEmail("john.doe@bankcore360.com")).thenReturn(Optional.of(sampleCustomer));
        when(bankAccountRepository.existsByCustomerIdAndAccountTypeAndAccountStatusIn(eq(1L), eq(AccountType.SAVINGS), any()))
                .thenReturn(false);
        when(accountNumberGenerator.generateUniqueAccountNumber(AccountType.SAVINGS)).thenReturn("360101234567");

        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount b = inv.getArgument(0);
            b.setId(101L);
            return b;
        });

        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        BankAccountResponse response = bankAccountService.createAccount(request, customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getAccountNumber()).isEqualTo("360101234567");
        assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.getCustomerId()).isEqualTo(1L);

        ArgumentCaptor<AccountAuditLog> auditCaptor = ArgumentCaptor.forClass(AccountAuditLog.class);
        verify(accountAuditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("ACCOUNT_CREATED");
    }

    @Test
    @DisplayName("Should create CURRENT account with PENDING status when customer KYC is not verified")
    void shouldCreateCurrentAccountWithPendingStatusWhenKycPending() {
        sampleCustomer.setKycStatus(KycStatus.PENDING);
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(sampleUser));
        when(customerRepository.findByEmail("john.doe@bankcore360.com")).thenReturn(Optional.of(sampleCustomer));
        when(bankAccountRepository.existsByCustomerIdAndAccountTypeAndAccountStatusIn(eq(1L), eq(AccountType.CURRENT), any()))
                .thenReturn(false);
        when(accountNumberGenerator.generateUniqueAccountNumber(AccountType.CURRENT)).thenReturn("360207654321");

        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount b = inv.getArgument(0);
            b.setId(102L);
            return b;
        });

        CreateAccountRequest request = new CreateAccountRequest(AccountType.CURRENT);

        BankAccountResponse response = bankAccountService.createAccount(request, customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getAccountType()).isEqualTo(AccountType.CURRENT);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should reject duplicate account creation if customer already has active or pending account of same type")
    void shouldRejectDuplicateAccountCreation() {
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(sampleUser));
        when(customerRepository.findByEmail("john.doe@bankcore360.com")).thenReturn(Optional.of(sampleCustomer));
        when(bankAccountRepository.existsByCustomerIdAndAccountTypeAndAccountStatusIn(eq(1L), eq(AccountType.SAVINGS), any()))
                .thenReturn(true);

        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        assertThatThrownBy(() -> bankAccountService.createAccount(request, customerAuth))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("Customer already has an active or pending SAVINGS account");

        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow customer to retrieve own accounts")
    void shouldAllowCustomerToRetrieveOwnAccounts() {
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.of(sampleUser));
        when(customerRepository.findByEmail("john.doe@bankcore360.com")).thenReturn(Optional.of(sampleCustomer));

        BankAccount account = new BankAccount("360101234567", sampleCustomer, AccountType.SAVINGS, AccountStatus.ACTIVE);
        account.setId(50L);
        when(bankAccountRepository.findByCustomerOrderByCreatedAtDesc(sampleCustomer)).thenReturn(List.of(account));

        List<BankAccountResponse> myAccounts = bankAccountService.getMyAccounts(customerAuth);

        assertThat(myAccounts).hasSize(1);
        assertThat(myAccounts.get(0).getAccountNumber()).isEqualTo("360101234567");
    }

    @Test
    @DisplayName("Should allow customer to retrieve own account by account number")
    void shouldAllowCustomerToRetrieveOwnAccountByNumber() {
        BankAccount account = new BankAccount("360101234567", sampleCustomer, AccountType.SAVINGS, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByAccountNumber("360101234567")).thenReturn(Optional.of(account));

        BankAccountResponse response = bankAccountService.getAccountByNumber("360101234567", customerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("360101234567");
    }

    @Test
    @DisplayName("Should forbid customer from viewing another customer's account by account number")
    void shouldForbidCustomerFromViewingAnotherCustomerAccount() {
        BankAccount account = new BankAccount("360109999999", anotherCustomer, AccountType.SAVINGS, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByAccountNumber("360109999999")).thenReturn(Optional.of(account));
        when(appUserRepository.findByUsername("john.doe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.getAccountByNumber("360109999999", customerAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to view another customer's bank account");
    }

    @Test
    @DisplayName("Should allow staff (MANAGER) to view any customer account")
    void shouldAllowStaffToViewAnyAccount() {
        BankAccount account = new BankAccount("360109999999", anotherCustomer, AccountType.SAVINGS, AccountStatus.ACTIVE);
        when(bankAccountRepository.findByAccountNumber("360109999999")).thenReturn(Optional.of(account));

        BankAccountResponse response = bankAccountService.getAccountByNumber("360109999999", managerAuth);

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("360109999999");
    }

    @Test
    @DisplayName("Should activate PENDING account when customer KYC is VERIFIED")
    void shouldActivatePendingAccountWhenCustomerKycVerified() {
        BankAccount account = new BankAccount("360101234567", sampleCustomer, AccountType.SAVINGS, AccountStatus.PENDING);
        when(bankAccountRepository.findByAccountNumber("360101234567")).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        BankAccountResponse response = bankAccountService.activateAccount("360101234567", managerAuth);

        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        ArgumentCaptor<AccountAuditLog> auditCaptor = ArgumentCaptor.forClass(AccountAuditLog.class);
        verify(accountAuditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("ACCOUNT_ACTIVATED");
        assertThat(auditCaptor.getValue().getPreviousStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(auditCaptor.getValue().getNewStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should reject activation when customer KYC is not VERIFIED")
    void shouldRejectActivationWhenCustomerKycNotVerified() {
        sampleCustomer.setKycStatus(KycStatus.UNDER_REVIEW);
        BankAccount account = new BankAccount("360101234567", sampleCustomer, AccountType.SAVINGS, AccountStatus.PENDING);
        when(bankAccountRepository.findByAccountNumber("360101234567")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bankAccountService.activateAccount("360101234567", managerAuth))
                .isInstanceOf(InvalidAccountStateException.class)
                .hasMessageContaining("Customer KYC status is UNDER_REVIEW, must be VERIFIED");

        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject activation when account is FROZEN or CLOSED")
    void shouldRejectActivationWhenAccountFrozenOrClosed() {
        BankAccount frozenAccount = new BankAccount("360101234567", sampleCustomer, AccountType.SAVINGS, AccountStatus.FROZEN);
        when(bankAccountRepository.findByAccountNumber("360101234567")).thenReturn(Optional.of(frozenAccount));

        assertThatThrownBy(() -> bankAccountService.activateAccount("360101234567", managerAuth))
                .isInstanceOf(InvalidAccountStateException.class)
                .hasMessageContaining("Account is currently FROZEN. Only PENDING accounts can be activated");

        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void shouldThrowWhenAccountNotFound() {
        when(bankAccountRepository.findByAccountNumber("999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.activateAccount("999999999999", managerAuth))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with number 999999999999 not found");
    }
}
