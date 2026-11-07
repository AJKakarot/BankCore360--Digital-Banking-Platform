package com.bankcore360.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class InjectCapitalRequest {

    @NotNull(message = "Injection amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum injection amount is ₹1,000.00")
    private BigDecimal amount;

    private String remarks;

    public InjectCapitalRequest() {
    }

    public InjectCapitalRequest(BigDecimal amount, String remarks) {
        this.amount = amount;
        this.remarks = remarks;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
