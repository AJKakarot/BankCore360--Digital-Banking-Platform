package com.bankcore360.entity;

/**
 * Status lifecycle transitions for a Loan Application.
 *
 * SUBMITTED -> UNDER_REVIEW -> DOCUMENTS_VERIFIED -> PENDING_MANAGER_APPROVAL -> APPROVED -> DISBURSED
 * Rejection can occur during document review or manager review: -> REJECTED
 */
public enum LoanStatus {
    SUBMITTED,
    UNDER_REVIEW,
    DOCUMENTS_VERIFIED,
    PENDING_MANAGER_APPROVAL,
    APPROVED,
    REJECTED,
    DISBURSED
}

