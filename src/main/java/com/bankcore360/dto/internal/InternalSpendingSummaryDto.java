package com.bankcore360.dto.internal;

import java.math.BigDecimal;
import java.util.Map;

public class InternalSpendingSummaryDto {
    private String customerUsername;
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private BigDecimal netSavings;
    private int transactionCount;
    private Map<String, BigDecimal> categoryBreakdown;
    private String highestSpendingCategory;
    private String monthOverMonthComparison;

    public InternalSpendingSummaryDto() {}

    public InternalSpendingSummaryDto(String customerUsername, BigDecimal totalInflow, BigDecimal totalOutflow,
                                      BigDecimal netSavings, int transactionCount, Map<String, BigDecimal> categoryBreakdown,
                                      String highestSpendingCategory, String monthOverMonthComparison) {
        this.customerUsername = customerUsername;
        this.totalInflow = totalInflow;
        this.totalOutflow = totalOutflow;
        this.netSavings = netSavings;
        this.transactionCount = transactionCount;
        this.categoryBreakdown = categoryBreakdown;
        this.highestSpendingCategory = highestSpendingCategory;
        this.monthOverMonthComparison = monthOverMonthComparison;
    }

    public String getCustomerUsername() { return customerUsername; }
    public void setCustomerUsername(String customerUsername) { this.customerUsername = customerUsername; }

    public BigDecimal getTotalInflow() { return totalInflow; }
    public void setTotalInflow(BigDecimal totalInflow) { this.totalInflow = totalInflow; }

    public BigDecimal getTotalOutflow() { return totalOutflow; }
    public void setTotalOutflow(BigDecimal totalOutflow) { this.totalOutflow = totalOutflow; }

    public BigDecimal getNetSavings() { return netSavings; }
    public void setNetSavings(BigDecimal netSavings) { this.netSavings = netSavings; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public Map<String, BigDecimal> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public String getHighestSpendingCategory() { return highestSpendingCategory; }
    public void setHighestSpendingCategory(String highestSpendingCategory) { this.highestSpendingCategory = highestSpendingCategory; }

    public String getMonthOverMonthComparison() { return monthOverMonthComparison; }
    public void setMonthOverMonthComparison(String monthOverMonthComparison) { this.monthOverMonthComparison = monthOverMonthComparison; }
}
