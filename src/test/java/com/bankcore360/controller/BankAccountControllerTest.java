package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.BankAccountResponse;
import com.bankcore360.dto.CreateAccountRequest;
import com.bankcore360.entity.AccountStatus;
import com.bankcore360.entity.AccountType;
import com.bankcore360.exception.AccountNotFoundException;
import com.bankcore360.exception.DuplicateAccountException;
import com.bankcore360.exception.InvalidAccountStateException;
import com.bankcore360.service.BankAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankAccountController.class)
@Import(SecurityConfig.class)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankAccountService bankAccountService;

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/accounts with CUSTOMER role should return 201 and created account")
    void shouldCreateSavingsAccountSuccessfully() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);
        BankAccountResponse response = new BankAccountResponse(
                1L, "360101234567", 10L, "John Doe",
                AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, LocalDateTime.now()
        );

        when(bankAccountService.createAccount(any(CreateAccountRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("360101234567"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("POST /api/accounts with duplicate account should return 409 Conflict")
    void shouldReturnConflictOnDuplicateAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        when(bankAccountService.createAccount(any(CreateAccountRequest.class), any()))
                .thenThrow(new DuplicateAccountException("Customer already has an active or pending SAVINGS account"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Customer already has an active or pending SAVINGS account"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("POST /api/accounts with CHECKER role should return 403 Forbidden")
    void shouldForbidCheckerFromCreatingAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /api/accounts without authentication should return 401 Unauthorized")
    void shouldReturnUnauthorizedWhenAnonymousCreatesAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts/my-accounts with CUSTOMER role should return 200 and list of accounts")
    void shouldReturnMyAccounts() throws Exception {
        BankAccountResponse account = new BankAccountResponse(
                1L, "360101234567", 10L, "John Doe",
                AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, LocalDateTime.now()
        );

        when(bankAccountService.getMyAccounts(any())).thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts/my-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value("360101234567"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts/{accountNumber} for own account should return 200")
    void shouldReturnAccountDetailsForOwner() throws Exception {
        BankAccountResponse account = new BankAccountResponse(
                1L, "360101234567", 10L, "John Doe",
                AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, LocalDateTime.now()
        );

        when(bankAccountService.getAccountByNumber(eq("360101234567"), any())).thenReturn(account);

        mockMvc.perform(get("/api/accounts/360101234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("360101234567"));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts/{accountNumber} for another customer should return 403 Forbidden")
    void shouldForbidCustomerFromAccessingAnotherCustomerAccount() throws Exception {
        when(bankAccountService.getAccountByNumber(eq("360109999999"), any()))
                .thenThrow(new AccessDeniedException("Access denied: You are not authorized to view another customer's bank account"));

        mockMvc.perform(get("/api/accounts/360109999999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dev_customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts/{accountNumber} when account not found should return 404")
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(bankAccountService.getAccountByNumber(eq("360999999999"), any()))
                .thenThrow(new AccountNotFoundException("Account with number 360999999999 not found"));

        mockMvc.perform(get("/api/accounts/360999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account with number 360999999999 not found"));
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("PATCH /api/accounts/{accountNumber}/activate with MANAGER role should return 200")
    void shouldAllowManagerToActivateAccount() throws Exception {
        BankAccountResponse account = new BankAccountResponse(
                1L, "360101234567", 10L, "John Doe",
                AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, LocalDateTime.now()
        );

        when(bankAccountService.activateAccount(eq("360101234567"), any())).thenReturn(account);

        mockMvc.perform(patch("/api/accounts/360101234567/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "dev_admin", roles = {"ADMIN"})
    @DisplayName("PATCH /api/accounts/{accountNumber}/activate with ADMIN role should return 200")
    void shouldAllowAdminToActivateAccount() throws Exception {
        BankAccountResponse account = new BankAccountResponse(
                1L, "360101234567", 10L, "John Doe",
                AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, LocalDateTime.now()
        );

        when(bankAccountService.activateAccount(eq("360101234567"), any())).thenReturn(account);

        mockMvc.perform(patch("/api/accounts/360101234567/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "dev_checker", roles = {"CHECKER"})
    @DisplayName("PATCH /api/accounts/{accountNumber}/activate with CHECKER role should return 403 Forbidden")
    void shouldForbidCheckerFromActivatingAccount() throws Exception {
        mockMvc.perform(patch("/api/accounts/360101234567/activate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dev_manager", roles = {"MANAGER"})
    @DisplayName("PATCH /api/accounts/{accountNumber}/activate when KYC not verified should return 400 Bad Request")
    void shouldReturnBadRequestWhenActivatingWithPendingKyc() throws Exception {
        when(bankAccountService.activateAccount(eq("360101234567"), any()))
                .thenThrow(new InvalidAccountStateException("Cannot activate account. Customer KYC status is PENDING, must be VERIFIED"));

        mockMvc.perform(patch("/api/accounts/360101234567/activate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot activate account. Customer KYC status is PENDING, must be VERIFIED"));
    }
}

