package com.bankcore360.dto.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InternalTransactionDetailDto {
    private Long id;
    private String transactionReference;
    private LocalDateTime timestamp;
    private BigDecimal amount;
    private String type;
    private String status;
    private String description;
    private String direction;
    private String accountNumber;
    private boolean reversed;
    private String reversalReason;
    private List<String> counterpartyDetails;

    public InternalTransactionDetailDto() {}

    public InternalTransactionDetailDto(Long id, String transactionReference, LocalDateTime timestamp,
                                        BigDecimal amount, String type, String status, String description,
                                        String direction, String accountNumber, boolean reversed,
                                        String reversalReason, List<String> counterpartyDetails) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.timestamp = timestamp;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.direction = direction;
        this.accountNumber = accountNumber;
        this.reversed = reversed;
        this.reversalReason = reversalReason;
        this.counterpartyDetails = counterpartyDetails;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }

    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }

    public List<String> getCounterpartyDetails() { return counterpartyDetails; }
    public void setCounterpartyDetails(List<String> counterpartyDetails) { this.counterpartyDetails = counterpartyDetails; }
}
