package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "bank_statements",
    indexes = {
        @Index(name = "idx_stmt_ref", columnList = "statement_reference", unique = true),
        @Index(name = "idx_stmt_hash", columnList = "file_hash_sha256", unique = true),
        @Index(name = "idx_stmt_account", columnList = "account_number")
    }
)
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_reference", nullable = false, unique = true, length = 64)
    private String statementReference;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_hash_sha256", nullable = false, unique = true, length = 64)
    private String fileHashSha256;

    @Column(name = "statement_start_date")
    private LocalDate statementStartDate;

    @Column(name = "statement_end_date")
    private LocalDate statementEndDate;

    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "total_records", nullable = false)
    private int totalRecords = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatementStatus status = StatementStatus.UPLOADED;

    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BankStatementItem> items = new ArrayList<>();

    public BankStatement() {
    }

    public BankStatement(String statementReference, String bankName, String accountNumber,
                         String filename, String fileHashSha256, String uploadedBy) {
        this.statementReference = statementReference;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.filename = filename;
        this.fileHashSha256 = fileHashSha256;
        this.uploadedBy = uploadedBy;
        this.status = StatementStatus.UPLOADED;
        this.uploadedAt = LocalDateTime.now();
    }

    public void addItem(BankStatementItem item) {
        items.add(item);
        item.setStatement(this);
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

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public void setStatus(StatementStatus status) {
        this.status = status;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public List<BankStatementItem> getItems() {
        return items;
    }

    public void setItems(List<BankStatementItem> items) {
        this.items = items;
    }
}
