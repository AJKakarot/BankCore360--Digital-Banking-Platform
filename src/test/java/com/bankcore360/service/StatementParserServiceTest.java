package com.bankcore360.service;

import com.bankcore360.dto.StatementUploadResponse;
import com.bankcore360.entity.BankStatement;
import com.bankcore360.repository.BankStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatementParserServiceTest {

    @Mock
    private BankStatementRepository bankStatementRepository;

    private StatementParserService statementParserService;

    @BeforeEach
    void setUp() {
        statementParserService = new StatementParserService(bankStatementRepository);
    }

    @Test
    @DisplayName("Should successfully parse valid CSV statement with multiple transactions")
    void testParseValidCsvStatement() {
        String csvContent = """
                Date,Reference,Description,Debit,Credit,Balance
                2026-11-01,TXN-EXT-001,Customer Deposit,,50000.00,50000.00
                2026-11-02,TXN-EXT-002,ATM Withdrawal,5000.00,,45000.00
                2026-11-03,TXN-EXT-003,Vendor Settlement,12000.00,,33000.00
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(bankStatementRepository.existsByFileHashSha256(anyString())).thenReturn(false);
        when(bankStatementRepository.save(any(BankStatement.class))).thenAnswer(invocation -> {
            BankStatement stmt = invocation.getArgument(0);
            stmt.setId(101L);
            return stmt;
        });

        StatementUploadResponse response = statementParserService.uploadAndParseStatement(
                file, "State Clearing Bank", "360100000001", "ADMIN"
        );

        assertNotNull(response);
        assertEquals(3, response.getTotalRecords());
        assertEquals("State Clearing Bank", response.getBankName());
        assertEquals("360100000001", response.getAccountNumber());
        assertNotNull(response.getFileHashSha256());
        verify(bankStatementRepository, times(1)).save(any(BankStatement.class));
    }

    @Test
    @DisplayName("Should reject duplicate statement upload idempotently via SHA-256 hash")
    void testRejectDuplicateStatementUpload() {
        String csvContent = "Date,Reference,Description,Debit,Credit,Balance\n2026-11-01,TXN-001,Test,,1000,1000";
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(bankStatementRepository.existsByFileHashSha256(anyString())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                statementParserService.uploadAndParseStatement(file, "Bank", "ACC1", "ADMIN")
        );

        assertTrue(ex.getMessage().contains("Duplicate statement detected"));
        verify(bankStatementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject empty or null file")
    void testRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                statementParserService.uploadAndParseStatement(emptyFile, "Bank", "ACC1", "ADMIN")
        );

        assertTrue(ex.getMessage().contains("cannot be empty"));
    }
}
