-- ============================================================================
-- BankCore360 Production Baseline Database Migration
-- Version: V1
-- Description: Baseline schema definition with strict referential integrity,
--              FinTech precision constraints, and high-performance indexes.
-- ============================================================================

-- 1. App Users & Authentication Subsystem
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    mfa_type VARCHAR(20),
    mfa_recovery_codes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON app_users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON app_users(email);

-- 2. Refresh Token Rotation & Session Tracking Subsystem
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_family VARCHAR(64) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_token_family ON refresh_tokens(token_family);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    session_token_hash VARCHAR(64) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    device_os VARCHAR(50),
    browser VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    suspicious BOOLEAN NOT NULL DEFAULT FALSE,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_active ON user_sessions(user_id, active);
CREATE INDEX IF NOT EXISTS idx_sessions_token_hash ON user_sessions(session_token_hash);

-- 3. Customers Subsystem
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customer_kyc_status ON customers(kyc_status);

-- 4. Customer KYC Subsystem
CREATE TABLE IF NOT EXISTS kyc_documents (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(100),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_kyc_doc_customer ON kyc_documents(customer_id);
CREATE INDEX IF NOT EXISTS idx_kyc_doc_status ON kyc_documents(verification_status);

CREATE TABLE IF NOT EXISTS kyc_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    reviewer_username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kyc_audit_customer ON kyc_audit_logs(customer_id);

CREATE TABLE IF NOT EXISTS kyc_document_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    reviewer_username VARCHAR(100) NOT NULL,
    rejection_reason VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kyc_doc_audit_doc ON kyc_document_audit_logs(document_id);

-- 5. Bank Accounts Subsystem
CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    account_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_accounts_number ON bank_accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON bank_accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON bank_accounts(account_status);

CREATE TABLE IF NOT EXISTS account_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_account_audit_num ON account_audit_logs(account_number);

-- 6. Core Double-Entry Ledger Subsystem
CREATE TABLE IF NOT EXISTS ledger_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    customer_id BIGINT REFERENCES customers(id) ON DELETE RESTRICT,
    is_system_account BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ledger_account_num ON ledger_accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_ledger_account_type ON ledger_accounts(account_type);

CREATE TABLE IF NOT EXISTS ledger_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(64) UNIQUE,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'POSTED',
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    description VARCHAR(500),
    initiated_by VARCHAR(100) NOT NULL,
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ledger_tx_ref ON ledger_transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_idempotency ON ledger_transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_posted_at ON ledger_transactions(posted_at);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    entry_reference VARCHAR(64) NOT NULL UNIQUE,
    transaction_id BIGINT NOT NULL REFERENCES ledger_transactions(id) ON DELETE RESTRICT,
    account_id BIGINT NOT NULL REFERENCES ledger_accounts(id) ON DELETE RESTRICT,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    entry_order INT NOT NULL,
    narration VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_entry_reference ON ledger_entries(entry_reference);
CREATE INDEX IF NOT EXISTS idx_entry_account ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_entry_transaction ON ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_entry_created_at ON ledger_entries(created_at);

CREATE TABLE IF NOT EXISTS account_balances (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL UNIQUE REFERENCES ledger_accounts(id) ON DELETE RESTRICT,
    current_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    pending_debit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    pending_credit NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    last_transaction_id BIGINT,
    last_transaction_reference VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_balance_account ON account_balances(account_id);

CREATE TABLE IF NOT EXISTS transaction_reversals (
    id BIGSERIAL PRIMARY KEY,
    original_transaction_id BIGINT NOT NULL UNIQUE REFERENCES ledger_transactions(id) ON DELETE RESTRICT,
    reversal_transaction_id BIGINT NOT NULL UNIQUE REFERENCES ledger_transactions(id) ON DELETE RESTRICT,
    reason VARCHAR(500) NOT NULL,
    reversed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reversal_original ON transaction_reversals(original_transaction_id);
CREATE INDEX IF NOT EXISTS idx_reversal_new ON transaction_reversals(reversal_transaction_id);

-- 7. Bank Vault & Treasury Subsystem
CREATE TABLE IF NOT EXISTS bank_vaults (
    id BIGSERIAL PRIMARY KEY,
    vault_name VARCHAR(100) NOT NULL UNIQUE,
    total_capital NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    available_liquidity NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_disbursed NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_repaid NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vault_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(64) UNIQUE,
    type VARCHAR(30) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_before NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    related_reference VARCHAR(64),
    beneficiary_account_number VARCHAR(64),
    beneficiary_name VARCHAR(150),
    performed_by VARCHAR(100) NOT NULL,
    remarks VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vault_tx_ref ON vault_transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_vault_tx_idempotency ON vault_transactions(idempotency_key);

-- 8. Loan Management Subsystem
CREATE TABLE IF NOT EXISTS loan_applications (
    id BIGSERIAL PRIMARY KEY,
    application_reference VARCHAR(64) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    loan_type VARCHAR(30) NOT NULL,
    requested_amount NUMERIC(15, 2) NOT NULL,
    tenure_months INT NOT NULL,
    purpose VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    approved_amount NUMERIC(15, 2),
    interest_rate NUMERIC(5, 2),
    approved_tenure_months INT,
    manager_remarks VARCHAR(500),
    reviewed_by VARCHAR(100),
    decision_timestamp TIMESTAMP,
    disbursed_at TIMESTAMP,
    disbursement_reference VARCHAR(64),
    disbursed_by VARCHAR(100),
    rejection_reason VARCHAR(500),
    rejected_by VARCHAR(100),
    rejected_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_app_ref ON loan_applications(application_reference);
CREATE INDEX IF NOT EXISTS idx_loan_customer ON loan_applications(customer_id);
CREATE INDEX IF NOT EXISTS idx_loan_status ON loan_applications(status);

CREATE TABLE IF NOT EXISTS loan_documents (
    id BIGSERIAL PRIMARY KEY,
    loan_application_id BIGINT NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(30) NOT NULL,
    storage_reference VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(100),
    document_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    remarks VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_loan_doc_app ON loan_documents(loan_application_id);

CREATE TABLE IF NOT EXISTS loan_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    application_reference VARCHAR(64) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    remarks VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_audit_app_ref ON loan_audit_logs(application_reference);

CREATE TABLE IF NOT EXISTS loan_repayments (
    id BIGSERIAL PRIMARY KEY,
    loan_application_id BIGINT NOT NULL REFERENCES loan_applications(id) ON DELETE RESTRICT,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    emi_amount NUMERIC(15, 2) NOT NULL,
    principal_component NUMERIC(15, 2) NOT NULL,
    interest_component NUMERIC(15, 2) NOT NULL,
    outstanding_balance NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    late_penalty_amount NUMERIC(15, 2) DEFAULT 0.00,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_app ON loan_repayments(loan_application_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_status ON loan_repayments(status);

-- 9. Real-Time Fraud Detection Subsystem
CREATE TABLE IF NOT EXISTS fraud_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    rule_type VARCHAR(50) NOT NULL,
    weight_score INT NOT NULL DEFAULT 10,
    threshold_value NUMERIC(15, 2) NOT NULL,
    time_window_seconds INT,
    max_allowed_events INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_rules_code ON fraud_rules(rule_code);

CREATE TABLE IF NOT EXISTS fraud_assessments (
    id BIGSERIAL PRIMARY KEY,
    assessment_reference VARCHAR(64) NOT NULL UNIQUE,
    account_number VARCHAR(64),
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    transaction_type VARCHAR(50),
    amount NUMERIC(19, 4) NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    triggered_rule_codes TEXT,
    assessment_reasons TEXT,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_assess_ref ON fraud_assessments(assessment_reference);
CREATE INDEX IF NOT EXISTS idx_fraud_assess_account ON fraud_assessments(account_number);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_reference VARCHAR(64) NOT NULL UNIQUE,
    assessment_id BIGINT REFERENCES fraud_assessments(id) ON DELETE SET NULL,
    account_number VARCHAR(64),
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    transaction_reference VARCHAR(64),
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_reviewer VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_alert_ref ON fraud_alerts(alert_reference);
CREATE INDEX IF NOT EXISTS idx_fraud_alert_status ON fraud_alerts(status);

CREATE TABLE IF NOT EXISTS fraud_investigations (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL UNIQUE REFERENCES fraud_alerts(id) ON DELETE CASCADE,
    investigator_username VARCHAR(100) NOT NULL,
    action_taken VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Bank Statement Reconciliation Subsystem
CREATE TABLE IF NOT EXISTS bank_statements (
    id BIGSERIAL PRIMARY KEY,
    statement_reference VARCHAR(64) NOT NULL UNIQUE,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_hash_sha256 VARCHAR(64) NOT NULL UNIQUE,
    statement_start_date DATE NOT NULL,
    statement_end_date DATE NOT NULL,
    opening_balance NUMERIC(19, 4) NOT NULL,
    closing_balance NUMERIC(19, 4) NOT NULL,
    total_records INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_statement_ref ON bank_statements(statement_reference);
CREATE INDEX IF NOT EXISTS idx_statement_hash ON bank_statements(file_hash_sha256);

CREATE TABLE IF NOT EXISTS bank_statement_items (
    id BIGSERIAL PRIMARY KEY,
    statement_id BIGINT NOT NULL REFERENCES bank_statements(id) ON DELETE CASCADE,
    item_reference VARCHAR(64) NOT NULL UNIQUE,
    transaction_date DATE NOT NULL,
    value_date DATE NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    description VARCHAR(500),
    external_reference VARCHAR(100),
    balance_after NUMERIC(19, 4),
    reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    matched_ledger_entry_id BIGINT,
    matched_ledger_txn_ref VARCHAR(64),
    matched_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_statement_item_ref ON bank_statement_items(item_reference);
CREATE INDEX IF NOT EXISTS idx_statement_item_stmt ON bank_statement_items(statement_id);
CREATE INDEX IF NOT EXISTS idx_statement_item_status ON bank_statement_items(reconciliation_status);

CREATE TABLE IF NOT EXISTS reconciliation_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_reference VARCHAR(64) NOT NULL UNIQUE,
    statement_id BIGINT NOT NULL REFERENCES bank_statements(id) ON DELETE RESTRICT,
    reconciled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reconciled_by VARCHAR(100) NOT NULL,
    matched_count INT NOT NULL DEFAULT 0,
    unmatched_statement_count INT NOT NULL DEFAULT 0,
    unmatched_ledger_count INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED'
);

CREATE INDEX IF NOT EXISTS idx_recon_batch_ref ON reconciliation_batches(batch_reference);

CREATE TABLE IF NOT EXISTS reconciliation_discrepancies (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES reconciliation_batches(id) ON DELETE CASCADE,
    statement_item_id BIGINT NOT NULL REFERENCES bank_statement_items(id) ON DELETE CASCADE,
    ledger_entry_id BIGINT,
    ledger_txn_reference VARCHAR(64),
    discrepancy_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    statement_amount NUMERIC(19, 4),
    ledger_amount NUMERIC(19, 4),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    investigator_notes TEXT,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recon_disc_batch ON reconciliation_discrepancies(batch_id);
CREATE INDEX IF NOT EXISTS idx_recon_disc_item ON reconciliation_discrepancies(statement_item_id);
CREATE INDEX IF NOT EXISTS idx_recon_disc_status ON reconciliation_discrepancies(status);

-- 11. Customer Notifications Subsystem
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_username VARCHAR(100) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    related_reference VARCHAR(64),
    action_url VARCHAR(255),
    action_label VARCHAR(100),
    action_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_username);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(recipient_username, read_status);

-- 12. Transactional Outbox & Event Streaming Subsystem
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    topic VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

CREATE TABLE IF NOT EXISTS event_outbox_records (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    last_error VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_event_outbox_status_created ON event_outbox_records(status, created_at);
CREATE INDEX IF NOT EXISTS idx_event_outbox_event_id ON event_outbox_records(event_id);
CREATE INDEX IF NOT EXISTS idx_event_outbox_aggregate ON event_outbox_records(aggregate_id);

CREATE TABLE IF NOT EXISTS processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_event_group ON processed_events(event_id, consumer_group);

