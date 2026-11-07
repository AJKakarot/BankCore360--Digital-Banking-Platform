package com.bankcore360.service;

import com.bankcore360.dto.BankAccountResponse;
import com.bankcore360.dto.CreateAccountRequest;
import com.bankcore360.entity.*;
import com.bankcore360.exception.AccountNotFoundException;
import com.bankcore360.exception.CustomerNotFoundException;
import com.bankcore360.exception.DuplicateAccountException;
import com.bankcore360.exception.InvalidAccountStateException;
import com.bankcore360.repository.AccountAuditLogRepository;
import com.bankcore360.repository.AppUserRepository;
import com.bankcore360.repository.BankAccountRepository;
import com.bankcore360.repository.CustomerRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountAuditLogRepository accountAuditLogRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.bankcore360.repository.VaultTransactionRepository vaultTransactionRepository;

    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            AccountAuditLogRepository accountAuditLogRepository,
            CustomerRepository customerRepository,
            AppUserRepository appUserRepository,
            AccountNumberGenerator accountNumberGenerator
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.accountAuditLogRepository = accountAuditLogRepository;
        this.customerRepository = customerRepository;
        this.appUserRepository = appUserRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Transactional
    public BankAccountResponse createAccount(CreateAccountRequest request, Authentication auth) {
        Customer customer = resolveCustomerFromAuth(auth);

        // Check for existing pending or active account of same type
        boolean alreadyExists = bankAccountRepository.existsByCustomerIdAndAccountTypeAndAccountStatusIn(
                customer.getId(),
                request.getAccountType(),
                List.of(AccountStatus.PENDING, AccountStatus.ACTIVE)
        );
        if (alreadyExists) {
            throw new DuplicateAccountException(
                    "Customer already has an active or pending " + request.getAccountType() + " account"
            );
        }

        // Active only if customer KYC is verified, else pending
        AccountStatus initialStatus = (customer.getKycStatus() == KycStatus.VERIFIED)
                ? AccountStatus.ACTIVE
                : AccountStatus.PENDING;

        String accountNumber = accountNumberGenerator.generateUniqueAccountNumber(request.getAccountType());

        BankAccount account = new BankAccount(
                accountNumber,
                customer,
                request.getAccountType(),
                BigDecimal.ZERO,
                initialStatus
        );

        BankAccount savedAccount = bankAccountRepository.save(account);

        // Audit creation
        AccountAuditLog auditLog = new AccountAuditLog(
                savedAccount.getAccountNumber(),
                customer.getId(),
                null,
                initialStatus,
                "ACCOUNT_CREATED",
                auth.getName()
        );
        accountAuditLogRepository.save(auditLog);

        return mapToResponse(savedAccount);
    }

    @Transactional
    public List<BankAccountResponse> getMyAccounts(Authentication auth) {
        Customer customer = resolveCustomerFromAuth(auth);
        List<BankAccount> list = bankAccountRepository.findByCustomerOrderByCreatedAtDesc(customer);
        if (list.isEmpty()) {
            String accNum = "3601" + String.format("%08d", customer.getId() != null ? customer.getId() : 1);
            BankAccount defaultAcc = new BankAccount(
                    accNum,
                    customer,
                    AccountType.SAVINGS,
                    BigDecimal.ZERO,
                    AccountStatus.ACTIVE
            );
            list = List.of(bankAccountRepository.save(defaultAcc));
        }
        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.bankcore360.entity.VaultTransaction> getMyTransactions(Authentication auth) {
        Customer customer = resolveCustomerFromAuth(auth);
        if (vaultTransactionRepository == null) return List.of();
        String accNum = "3601" + String.format("%08d", customer.getId() != null ? customer.getId() : 1);
        return vaultTransactionRepository.findByBeneficiaryAccountNumberOrderByTimestampDesc(accNum);
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getAccountByNumber(String accountNumber, Authentication auth) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account with number " + accountNumber + " not found"));

        validateAccountAccess(auth, account);

        return mapToResponse(account);
    }

    @Transactional
    public BankAccountResponse activateAccount(String accountNumber, Authentication auth) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account with number " + accountNumber + " not found"));

        Customer customer = account.getCustomer();
        if (customer.getKycStatus() != KycStatus.VERIFIED) {
            throw new InvalidAccountStateException(
                    "Cannot activate account. Customer KYC status is " + customer.getKycStatus() + ", must be VERIFIED"
            );
        }

        if (account.getAccountStatus() == AccountStatus.FROZEN || account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(
                    "Cannot activate account. Account is currently " + account.getAccountStatus() + ". Only PENDING accounts can be activated."
            );
        }

        if (account.getAccountStatus() == AccountStatus.ACTIVE) {
            return mapToResponse(account);
        }

        AccountStatus previousStatus = account.getAccountStatus();
        account.setAccountStatus(AccountStatus.ACTIVE);
        BankAccount savedAccount = bankAccountRepository.save(account);

        String performedBy = (auth != null && auth.getName() != null) ? auth.getName() : "SYSTEM";
        AccountAuditLog auditLog = new AccountAuditLog(
                savedAccount.getAccountNumber(),
                customer.getId(),
                previousStatus,
                AccountStatus.ACTIVE,
                "ACCOUNT_ACTIVATED",
                performedBy
        );
        accountAuditLogRepository.save(auditLog);

        return mapToResponse(savedAccount);
    }

    public Customer resolveCustomerFromAuth(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required to access bank accounts");
        }
        String principal = auth.getName().trim();

        // 1. If principal is an email, match directly by email
        if (principal.contains("@")) {
            Optional<Customer> byEmail = customerRepository.findByEmail(principal.toLowerCase());
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }

        // 2. Lookup user record to get registered email
        Optional<AppUser> userOpt = appUserRepository.findByUsername(principal);
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            Optional<Customer> byUserEmail = customerRepository.findByEmail(userOpt.get().getEmail().toLowerCase());
            if (byUserEmail.isPresent()) {
                return byUserEmail.get();
            }
        }

        // 3. Fallback: match local-part of customer email
        List<Customer> allCustomers = customerRepository.findAll();
        for (Customer c : allCustomers) {
            if (isSamePerson(principal, c.getEmail())) {
                return c;
            }
        }

        throw new CustomerNotFoundException("No customer record found matching authenticated user: " + principal);
    }

    private void validateAccountAccess(Authentication auth, BankAccount account) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required to view account details");
        }

        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHECKER")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (isStaff) {
            return;
        }

        Customer customer = account.getCustomer();
        if (!isSamePerson(auth.getName(), customer.getEmail())) {
            throw new AccessDeniedException("Access denied: You are not authorized to view another customer's bank account");
        }
    }

    private boolean isSamePerson(String principalName, String customerEmail) {
        if (principalName == null || customerEmail == null) {
            return false;
        }
        String p = principalName.trim().toLowerCase();
        String c = customerEmail.trim().toLowerCase();

        if (p.equals(c)) {
            return true;
        }

        int atIndex = c.indexOf('@');
        if (atIndex > 0) {
            String emailLocalPart = c.substring(0, atIndex);
            if (p.equals(emailLocalPart)) {
                return true;
            }
        }

        int pAtIndex = p.indexOf('@');
        if (pAtIndex > 0) {
            String principalLocalPart = p.substring(0, pAtIndex);
            if (principalLocalPart.equals(c)) {
                return true;
            }
            if (atIndex > 0 && principalLocalPart.equals(c.substring(0, atIndex))) {
                return true;
            }
        }

        Optional<AppUser> userOpt = appUserRepository.findByUsername(principalName);
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            return userOpt.get().getEmail().trim().equalsIgnoreCase(c);
        }

        return false;
    }

    private BankAccountResponse mapToResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomer().getId(),
                account.getCustomer().getFullName(),
                account.getAccountType(),
                account.getBalance(),
                account.getAccountStatus(),
                account.getCreatedAt()
        );
    }
}
