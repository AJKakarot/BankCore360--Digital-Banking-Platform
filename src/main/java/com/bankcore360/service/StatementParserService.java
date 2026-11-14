package com.bankcore360.service;

import com.bankcore360.dto.StatementUploadResponse;
import com.bankcore360.entity.BankStatement;
import com.bankcore360.entity.BankStatementItem;
import com.bankcore360.entity.LedgerEntryDirection;
import com.bankcore360.entity.StatementStatus;
import com.bankcore360.repository.BankStatementRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * High-Performance Bank Statement Ingestion Service supporting CSV and Excel (.xlsx) formats.
 * Implements cryptographic SHA-256 idempotency protection against duplicate uploads.
 */
@Service
public class StatementParserService {

    private static final Logger log = LoggerFactory.getLogger(StatementParserService.class);
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    );

    private final BankStatementRepository bankStatementRepository;

    public StatementParserService(BankStatementRepository bankStatementRepository) {
        this.bankStatementRepository = bankStatementRepository;
    }

    @Transactional
    public StatementUploadResponse uploadAndParseStatement(MultipartFile file, String bankName,
                                                          String accountNumber, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Bank statement file cannot be empty");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement.csv";
        byte[] fileBytes;
        String sha256;
        try {
            fileBytes = file.getBytes();
            sha256 = computeSha256(fileBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read uploaded statement file bytes", e);
        }

        // 1. Idempotency Check: Reject duplicate file upload
        if (bankStatementRepository.existsByFileHashSha256(sha256)) {
            throw new IllegalStateException("Duplicate statement detected: A file with exact SHA-256 checksum ("
                    + sha256.substring(0, 12) + "...) has already been ingested.");
        }

        String stmtRef = "STMT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        BankStatement statement = new BankStatement(stmtRef, bankName != null ? bankName : "External Bank",
                accountNumber != null ? accountNumber : "OPERATIVE-001", filename, sha256, uploadedBy);

        List<BankStatementItem> parsedItems;
        if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            parsedItems = parseExcel(fileBytes, statement);
        } else {
            parsedItems = parseCsv(fileBytes, statement);
        }

        if (parsedItems.isEmpty()) {
            throw new IllegalArgumentException("No valid transaction rows found in the uploaded statement");
        }

        // Calculate metadata dates & balances
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (BankStatementItem item : parsedItems) {
            statement.addItem(item);
            if (minDate == null || item.getTransactionDate().isBefore(minDate)) minDate = item.getTransactionDate();
            if (maxDate == null || item.getTransactionDate().isAfter(maxDate)) maxDate = item.getTransactionDate();
        }

        statement.setStatementStartDate(minDate);
        statement.setStatementEndDate(maxDate);
        statement.setTotalRecords(parsedItems.size());
        statement.setStatus(StatementStatus.UPLOADED);

        BankStatement saved = bankStatementRepository.save(statement);
        log.info("Successfully uploaded and parsed bank statement: {} with {} records", saved.getStatementReference(), saved.getTotalRecords());

        return new StatementUploadResponse(
                saved.getId(),
                saved.getStatementReference(),
                saved.getBankName(),
                saved.getAccountNumber(),
                saved.getFilename(),
                saved.getFileHashSha256(),
                saved.getTotalRecords(),
                saved.getOpeningBalance(),
                saved.getClosingBalance(),
                saved.getStatementStartDate(),
                saved.getStatementEndDate(),
                saved.getStatus().name(),
                "Bank statement uploaded and validated successfully. Ready for automated reconciliation matching."
        );
    }

    private List<BankStatementItem> parseCsv(byte[] bytes, BankStatement statement) {
        List<BankStatementItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build())) {

            int rowIdx = 1;
            for (CSVRecord record : csvParser) {
                try {
                    String dateStr = getFirstMatchingHeader(record, "Date", "Txn Date", "Transaction Date");
                    String refStr = getFirstMatchingHeader(record, "Reference", "Ref", "Txn Ref", "Cheque No", "Transaction ID");
                    String descStr = getFirstMatchingHeader(record, "Description", "Narration", "Particulars", "Details");
                    String debitStr = getFirstMatchingHeader(record, "Debit", "Withdrawal", "DR");
                    String creditStr = getFirstMatchingHeader(record, "Credit", "Deposit", "CR");
                    String amountStr = getFirstMatchingHeader(record, "Amount");
                    String typeStr = getFirstMatchingHeader(record, "Type", "Direction", "DR/CR");

                    LocalDate txnDate = parseDate(dateStr);
                    if (txnDate == null) txnDate = LocalDate.now();

                    BigDecimal amount = BigDecimal.ZERO;
                    LedgerEntryDirection direction = LedgerEntryDirection.CREDIT;

                    if (debitStr != null && !debitStr.isBlank() && parseBigDecimal(debitStr).compareTo(BigDecimal.ZERO) > 0) {
                        amount = parseBigDecimal(debitStr);
                        direction = LedgerEntryDirection.DEBIT;
                    } else if (creditStr != null && !creditStr.isBlank() && parseBigDecimal(creditStr).compareTo(BigDecimal.ZERO) > 0) {
                        amount = parseBigDecimal(creditStr);
                        direction = LedgerEntryDirection.CREDIT;
                    } else if (amountStr != null && !amountStr.isBlank()) {
                        amount = parseBigDecimal(amountStr).abs();
                        if (typeStr != null && (typeStr.toUpperCase().contains("DR") || typeStr.toUpperCase().contains("DEBIT"))) {
                            direction = LedgerEntryDirection.DEBIT;
                        } else {
                            direction = LedgerEntryDirection.CREDIT;
                        }
                    }

                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // Skip zero/empty rows
                    }

                    String itemRef = "SITM-" + rowIdx++;
                    String extRef = (refStr != null && !refStr.isBlank()) ? refStr.trim() : itemRef;
                    String description = descStr != null ? descStr.trim() : "External transaction";

                    BankStatementItem item = new BankStatementItem(itemRef, txnDate, amount, direction, description, extRef);
                    items.add(item);
                } catch (Exception rowEx) {
                    log.warn("Skipping unparseable CSV statement row: {}", rowEx.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV statement: " + e.getMessage(), e);
        }
        return items;
    }

    private List<BankStatementItem> parseExcel(byte[] bytes, BankStatement statement) {
        List<BankStatementItem> items = new ArrayList<>();
        try (InputStream is = new java.io.ByteArrayInputStream(bytes);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return items;

            // Header mapping
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }

            int rowIdx = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    String dateStr = getCellValue(row, headerMap, "date", "txn date", "transaction date");
                    String refStr = getCellValue(row, headerMap, "reference", "ref", "txn ref", "cheque no", "transaction id");
                    String descStr = getCellValue(row, headerMap, "description", "narration", "particulars");
                    String debitStr = getCellValue(row, headerMap, "debit", "withdrawal", "dr");
                    String creditStr = getCellValue(row, headerMap, "credit", "deposit", "cr");
                    String amountStr = getCellValue(row, headerMap, "amount");

                    LocalDate txnDate = parseDate(dateStr);
                    if (txnDate == null) txnDate = LocalDate.now();

                    BigDecimal amount = BigDecimal.ZERO;
                    LedgerEntryDirection direction = LedgerEntryDirection.CREDIT;

                    if (debitStr != null && !debitStr.isBlank() && parseBigDecimal(debitStr).compareTo(BigDecimal.ZERO) > 0) {
                        amount = parseBigDecimal(debitStr);
                        direction = LedgerEntryDirection.DEBIT;
                    } else if (creditStr != null && !creditStr.isBlank() && parseBigDecimal(creditStr).compareTo(BigDecimal.ZERO) > 0) {
                        amount = parseBigDecimal(creditStr);
                        direction = LedgerEntryDirection.CREDIT;
                    } else if (amountStr != null && !amountStr.isBlank()) {
                        amount = parseBigDecimal(amountStr).abs();
                        direction = LedgerEntryDirection.CREDIT;
                    }

                    if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                    String itemRef = "SITM-" + rowIdx++;
                    String extRef = (refStr != null && !refStr.isBlank()) ? refStr.trim() : itemRef;
                    String description = descStr != null ? descStr.trim() : "Bank transaction";

                    BankStatementItem item = new BankStatementItem(itemRef, txnDate, amount, direction, description, extRef);
                    items.add(item);
                } catch (Exception rowEx) {
                    log.warn("Skipping unparseable Excel row: {}", rowEx.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Excel statement: " + e.getMessage(), e);
        }
        return items;
    }

    private String getFirstMatchingHeader(CSVRecord record, String... names) {
        for (String name : names) {
            if (record.isMapped(name)) {
                String val = record.get(name);
                if (val != null && !val.isBlank()) return val;
            }
        }
        return null;
    }

    private String getCellValue(Row row, Map<String, Integer> headerMap, String... names) {
        for (String name : names) {
            Integer col = headerMap.get(name);
            if (col != null) {
                Cell cell = row.getCell(col);
                if (cell != null) {
                    DataFormatter formatter = new DataFormatter();
                    return formatter.formatCellValue(cell).trim();
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        String clean = val.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            String clean = val.replaceAll("[^0-9.-]", "");
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256", e);
        }
    }
}

