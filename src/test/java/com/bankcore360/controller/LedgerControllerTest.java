package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import com.bankcore360.dto.*;
import com.bankcore360.entity.*;
import com.bankcore360.service.LedgerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
@Import(SecurityConfig.class)
public class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LedgerService ledgerService;

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("POST /api/ledger/deposit should record deposit and return 201 Created")
    void testDepositEndpoint() throws Exception {
        LedgerDepositRequest request = new LedgerDepositRequest(
                "ACC-1001",
                new BigDecimal("50000.00"),
                "IDEMP-DEP-101",
                "Deposit test"
        );

        LedgerTransaction txn = new LedgerTransaction(
                "TXN-DEP-101",
                "IDEMP-DEP-101",
                LedgerTransactionType.DEPOSIT,
                new BigDecimal("50000.00"),
                "INR",
                "Deposit test",
                "customer_user"
        );
        txn.setId(101L);
        LedgerTransactionResponse response = new LedgerTransactionResponse(txn);

        when(ledgerService.deposit(any(LedgerDepositRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/ledger/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.transactionReference").value("TXN-DEP-101"))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("POST /api/ledger/withdraw should record withdrawal and return 201 Created")
    void testWithdrawEndpoint() throws Exception {
        LedgerWithdrawRequest request = new LedgerWithdrawRequest(
                "ACC-1001",
                new BigDecimal("10000.00"),
                "IDEMP-WTD-101",
                "Withdrawal test"
        );

        LedgerTransaction txn = new LedgerTransaction(
                "TXN-WTD-101",
                "IDEMP-WTD-101",
                LedgerTransactionType.WITHDRAWAL,
                new BigDecimal("10000.00"),
                "INR",
                "Withdrawal test",
                "customer_user"
        );
        txn.setId(102L);
        LedgerTransactionResponse response = new LedgerTransactionResponse(txn);

        when(ledgerService.withdraw(any(LedgerWithdrawRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/ledger/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("POST /api/ledger/transfer should record transfer and return 201 Created")
    void testTransferEndpoint() throws Exception {
        LedgerTransferRequest request = new LedgerTransferRequest(
                "ACC-1001",
                "ACC-1002",
                new BigDecimal("5000.00"),
                "IDEMP-TRF-101",
                "P2P test"
        );

        LedgerTransaction txn = new LedgerTransaction(
                "TXN-TRF-101",
                "IDEMP-TRF-101",
                LedgerTransactionType.TRANSFER,
                new BigDecimal("5000.00"),
                "INR",
                "P2P test",
                "customer_user"
        );
        txn.setId(103L);
        LedgerTransactionResponse response = new LedgerTransactionResponse(txn);

        when(ledgerService.transfer(any(LedgerTransferRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(103))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("GET /api/ledger/accounts/{id}/entries should return ledger entries page")
    void testGetAccountEntries() throws Exception {
        LedgerAccount acc = new LedgerAccount("ACC-1001", "Savings", LedgerAccountType.LIABILITY, "INR",
                LedgerAccountStatus.ACTIVE, null, false);
        acc.setId(5L);

        LedgerEntry entry = new LedgerEntry("ENT-01", acc, LedgerEntryDirection.CREDIT,
                new BigDecimal("50000.00"), new BigDecimal("50000.00"), 1, "Initial credit");
        entry.setId(1L);

        PageImpl<LedgerEntryResponse> page = new PageImpl<>(List.of(new LedgerEntryResponse(entry)));
        when(ledgerService.getAccountEntries(eq(5L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/ledger/accounts/5/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entryReference").value("ENT-01"))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(50000.00));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"MANAGER"})
    @DisplayName("POST /api/ledger/transactions/{id}/reverse should execute reversal and return 201 Created")
    void testReverseTransactionEndpoint() throws Exception {
        LedgerReversalRequest request = new LedgerReversalRequest("Incorrect customer account credit", "REV-KEY-01");

        LedgerTransaction orig = new LedgerTransaction("TXN-DEP-999", "ORIG", LedgerTransactionType.DEPOSIT,
                new BigDecimal("5000.00"), "INR", "desc", "user");
        orig.setId(999L);
        LedgerTransaction rev = new LedgerTransaction("TXN-REV-999", "REV-KEY-01", LedgerTransactionType.REVERSAL,
                new BigDecimal("5000.00"), "INR", "reversal", "manager_user");
        rev.setId(1000L);

        TransactionReversal reversalRecord = new TransactionReversal(orig, rev, request.getReason(), "manager_user");
        reversalRecord.setId(10L);
        TransactionReversalResponse response = new TransactionReversalResponse(reversalRecord);

        when(ledgerService.reverseTransaction(eq(999L), any(LedgerReversalRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/ledger/transactions/999/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.originalTransactionId").value(999))
                .andExpect(jsonPath("$.reversalTransactionId").value(1000))
                .andExpect(jsonPath("$.reason").value("Incorrect customer account credit"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Unauthenticated request to /api/ledger/deposit should return 401 Unauthorized")
    void testUnauthenticatedAccessFails() throws Exception {
        LedgerDepositRequest request = new LedgerDepositRequest("ACC-1", new BigDecimal("100.00"), "K1", "desc");
        mockMvc.perform(post("/api/ledger/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

