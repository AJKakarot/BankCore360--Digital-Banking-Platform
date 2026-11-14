package com.bankcore360.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StatementUploadResponse {

    private Long id;
    private String statementReference;
    private String bankName;
    private String accountNumber;
    private String filename;
    private String fileHashSha256;
    private int totalRecords;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private String status;
    private String message;

    public StatementUploadResponse() {
    }

    public StatementUploadResponse(Long id, String statementReference, String bankName, String accountNumber,
                                   String filename, String fileHashSha256, int totalRecords,
                                   BigDecimal openingBalance, BigDecimal closingBalance,
                                   LocalDate statementStartDate, LocalDate statementEndDate,
                                   String status, String message) {
        this.id = id;
        this.statementReference = statementReference;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.filename = filename;
        this.fileHashSha256 = fileHashSha256;
        this.totalRecords = totalRecords;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.statementStartDate = statementStartDate;
        this.statementEndDate = statementEndDate;
        this.status = status;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatementReference() {
        return statementReference;
    }

    public void setStatementReference(String statementReference) {
        this.statementReference = statementReference;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileHashSha256() {
        return fileHashSha256;
    }

    public void setFileHashSha256(String fileHashSha256) {
        this.fileHashSha256 = fileHashSha256;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public LocalDate getStatementStartDate() {
        return statementStartDate;
    }

    public void setStatementStartDate(LocalDate statementStartDate) {
        this.statementStartDate = statementStartDate;
    }

    public LocalDate getStatementEndDate() {
        return statementEndDate;
    }

    public void setStatementEndDate(LocalDate statementEndDate) {
        this.statementEndDate = statementEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

