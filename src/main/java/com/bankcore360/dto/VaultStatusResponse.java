package com.bankcore360.dto;

import com.bankcore360.entity.VaultTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class VaultStatusResponse {

    private String vaultName;
    private BigDecimal totalCapital;
    private BigDecimal availableLiquidity;
    private BigDecimal totalDisbursed;
    private BigDecimal totalRepaid;
    private double utilizationPercent;
    private LocalDateTime lastUpdated;
    private List<VaultTransaction> recentTransactions;

    public VaultStatusResponse() {
    }

    public VaultStatusResponse(String vaultName,
                               BigDecimal totalCapital,
                               BigDecimal availableLiquidity,
                               BigDecimal totalDisbursed,
                               BigDecimal totalRepaid,
                               double utilizationPercent,
                               LocalDateTime lastUpdated,
                               List<VaultTransaction> recentTransactions) {
        this.vaultName = vaultName;
        this.totalCapital = totalCapital;
        this.availableLiquidity = availableLiquidity;
        this.totalDisbursed = totalDisbursed;
        this.totalRepaid = totalRepaid;
        this.utilizationPercent = utilizationPercent;
        this.lastUpdated = lastUpdated;
        this.recentTransactions = recentTransactions;
    }

    public String getVaultName() {
        return vaultName;
    }

    public BigDecimal getTotalCapital() {
        return totalCapital;
    }

    public BigDecimal getAvailableLiquidity() {
        return availableLiquidity;
    }

    public BigDecimal getTotalDisbursed() {
        return totalDisbursed;
    }

    public BigDecimal getTotalRepaid() {
        return totalRepaid;
    }

    public double getUtilizationPercent() {
        return utilizationPercent;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public List<VaultTransaction> getRecentTransactions() {
        return recentTransactions;
    }
}

