package com.bankcore360.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_vaults")
public class BankVault {

    public static final Long DEFAULT_VAULT_ID = 1L;
    public static final BigDecimal DEFAULT_INITIAL_CAPITAL = new BigDecimal("10000000.00"); // ₹100 Lakh (₹1 Crore)

    @Id
    private Long id = DEFAULT_VAULT_ID;

    @Column(nullable = false, length = 100)
    private String vaultName = "BankCore360 Central Treasury & Reserve";

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCapital = DEFAULT_INITIAL_CAPITAL;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableLiquidity = DEFAULT_INITIAL_CAPITAL;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDisbursed = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalRepaid = BigDecimal.ZERO;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public BankVault() {
    }

    public BankVault(Long id, String vaultName, BigDecimal totalCapital) {
        this.id = id;
        this.vaultName = vaultName;
        this.totalCapital = totalCapital;
        this.availableLiquidity = totalCapital;
        this.totalDisbursed = BigDecimal.ZERO;
        this.totalRepaid = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVaultName() {
        return vaultName;
    }

    public void setVaultName(String vaultName) {
        this.vaultName = vaultName;
    }

    public BigDecimal getTotalCapital() {
        return totalCapital;
    }

    public void setTotalCapital(BigDecimal totalCapital) {
        this.totalCapital = totalCapital;
    }

    public BigDecimal getAvailableLiquidity() {
        return availableLiquidity;
    }

    public void setAvailableLiquidity(BigDecimal availableLiquidity) {
        this.availableLiquidity = availableLiquidity;
    }

    public BigDecimal getTotalDisbursed() {
        return totalDisbursed;
    }

    public void setTotalDisbursed(BigDecimal totalDisbursed) {
        this.totalDisbursed = totalDisbursed;
    }

    public BigDecimal getTotalRepaid() {
        return totalRepaid;
    }

    public void setTotalRepaid(BigDecimal totalRepaid) {
        this.totalRepaid = totalRepaid;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
