package com.bankcore360.entity;

public enum LedgerAccountType {
    ASSET,       // Normal balance: DEBIT (e.g., Vault, Cash Reserve, Loans Receivable)
    LIABILITY,   // Normal balance: CREDIT (e.g., Customer Deposits, Borrowings)
    EQUITY,      // Normal balance: CREDIT (e.g., Bank Capital, Retained Earnings)
    REVENUE,     // Normal balance: CREDIT (e.g., Interest Income, Processing Fees)
    EXPENSE      // Normal balance: DEBIT (e.g., Operating Expenses, Interest Paid)
}

