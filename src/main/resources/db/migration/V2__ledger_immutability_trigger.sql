-- ============================================================================
-- BankCore360 Migration: V2__ledger_immutability_trigger.sql
-- Description: Enforces strict financial ledger immutability at the database engine level.
--              Prevents UPDATE and DELETE operations on posted ledger entries
--              and transaction reversal records.
-- ============================================================================

-- Function: prevent_ledger_entry_mutation
CREATE OR REPLACE FUNCTION prevent_ledger_entry_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'FINANCIAL LEDGER IMMUTABILITY VIOLATION: Ledger entries are permanently immutable and cannot be updated or deleted under banking regulatory compliance. (Attempted action: %, Entry ID: %)',
        TG_OP, OLD.id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger: enforce_ledger_entries_immutability
DROP TRIGGER IF EXISTS trg_ledger_entries_immutable ON ledger_entries;
CREATE TRIGGER trg_ledger_entries_immutable
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_ledger_entry_mutation();

-- Function: prevent_transaction_reversal_mutation
CREATE OR REPLACE FUNCTION prevent_transaction_reversal_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'TRANSACTION REVERSAL AUDIT IMMUTABILITY VIOLATION: Reversal records cannot be modified or deleted. (Attempted action: %, Reversal ID: %)',
        TG_OP, OLD.id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger: enforce_transaction_reversals_immutability
DROP TRIGGER IF EXISTS trg_transaction_reversals_immutable ON transaction_reversals;
CREATE TRIGGER trg_transaction_reversals_immutable
BEFORE UPDATE OR DELETE ON transaction_reversals
FOR EACH ROW
EXECUTE FUNCTION prevent_transaction_reversal_mutation();

