package com.bankcore360.dto.internal;

import java.math.BigDecimal;

public class InternalLoanProductDto {
    private String loanType;
    private String name;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int minTenureMonths;
    private int maxTenureMonths;
    private BigDecimal baseInterestRate;
    private boolean reducingBalance;
    private String description;

    public InternalLoanProductDto() {}

    public InternalLoanProductDto(String loanType, String name, BigDecimal minAmount, BigDecimal maxAmount,
                                  int minTenureMonths, int maxTenureMonths, BigDecimal baseInterestRate,
                                  boolean reducingBalance, String description) {
        this.loanType = loanType;
        this.name = name;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minTenureMonths = minTenureMonths;
        this.maxTenureMonths = maxTenureMonths;
        this.baseInterestRate = baseInterestRate;
        this.reducingBalance = reducingBalance;
        this.description = description;
    }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public int getMinTenureMonths() { return minTenureMonths; }
    public void setMinTenureMonths(int minTenureMonths) { this.minTenureMonths = minTenureMonths; }

    public int getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(int maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }

    public BigDecimal getBaseInterestRate() { return baseInterestRate; }
    public void setBaseInterestRate(BigDecimal baseInterestRate) { this.baseInterestRate = baseInterestRate; }

    public boolean isReducingBalance() { return reducingBalance; }
    public void setReducingBalance(boolean reducingBalance) { this.reducingBalance = reducingBalance; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
