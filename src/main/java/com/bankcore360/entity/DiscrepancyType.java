package com.bankcore360.entity;

public enum DiscrepancyType {
    AMOUNT_MISMATCH,
    DATE_OUT_OF_BOUNDS,
    MISSING_IN_LEDGER,
    MISSING_IN_STATEMENT,
    DUPLICATE_ENTRY,
    DIRECTION_MISMATCH
}
