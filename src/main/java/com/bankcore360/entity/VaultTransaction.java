package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vault_transactions", indexes = {
        @Index(name = "idx_vault_txn_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_vault_txn_ref", columnList = "transactionReference", unique = true),
        @Index(name = "idx_vault_txn_app_ref", columnList = "relatedReference")
})
public class VaultTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VaultTransactionType type;

    @Column(nullable = false, length = 10)
    private String direction; // "DEBIT" (Money Leaving Vault to Customer) or "CREDIT" (Money Entering Vault)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 64)
    private String relatedReference; // Loan Reference or Injection Reference

    @Column(length = 32)
    private String beneficiaryAccountNumber;

    @Column(length = 128)
    private String beneficiaryName;

    @Column(nullable = false, length = 64)
    private String performedBy;

    @Column(length = 255)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public VaultTransaction() {
    }

    public VaultTransaction(String transactionReference,
                            String idempotencyKey,
                            VaultTransactionType type,
                            String direction,
                            BigDecimal amount,
                            BigDecimal balanceBefore,
                            BigDecimal balanceAfter,
                            String relatedReference,
                            String beneficiaryAccountNumber,
                            String beneficiaryName,
                            String performedBy,
                            String remarks) {
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.relatedReference = relatedReference;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryName = beneficiaryName;
        this.performedBy = performedBy;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public VaultTransactionType getType() {
        return type;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public String getBeneficiaryAccountNumber() {
        return beneficiaryAccountNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
