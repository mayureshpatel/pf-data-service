-- V27: Data Integrity Constraints & Audit Columns
-- Purpose: Add audit trail and business constraints to prevent data quality issues

-- ====================================================================================
-- 1. ADD AUDIT COLUMNS
-- ====================================================================================
-- Add created_by, updated_by, deleted_by for audit trail
ALTER TABLE accounts
    ADD COLUMN created_by BIGINT REFERENCES users(id),
    ADD COLUMN updated_by BIGINT REFERENCES users(id),
    ADD COLUMN deleted_by BIGINT REFERENCES users(id);

-- Add indexes for audit queries
CREATE INDEX idx_accounts_created_by ON accounts (created_by);
CREATE INDEX idx_accounts_updated_by ON accounts (updated_by);

-- Add comments
COMMENT ON COLUMN accounts.created_by IS 'User ID who created this account';
COMMENT ON COLUMN accounts.updated_by IS 'User ID who last updated this account';
COMMENT ON COLUMN accounts.deleted_by IS 'User ID who soft-deleted this account';

-- ====================================================================================
-- 2. ADD UNIQUE CONSTRAINT ON ACCOUNT NAMES
-- ====================================================================================
-- Prevent duplicate account names per user (case-insensitive)
-- Allows name reuse after deletion (WHERE deleted_at IS NULL)
CREATE UNIQUE INDEX idx_accounts_user_name_unique
ON accounts (user_id, LOWER(name))
WHERE deleted_at IS NULL;

COMMENT ON INDEX idx_accounts_user_name_unique IS
'Ensures account names are unique per user (case-insensitive). Allows name reuse after soft deletion.';

-- ====================================================================================
-- 3. ADD BALANCE REASONABLENESS CHECK
-- ====================================================================================
-- Prevent extreme balance values (data quality check)
ALTER TABLE accounts
ADD CONSTRAINT chk_balance_reasonable
CHECK (current_balance BETWEEN -9999999999.99 AND 9999999999.99);

COMMENT ON CONSTRAINT chk_balance_reasonable ON accounts IS
'Prevents unreasonable balance values. Limit: ±$9.9 trillion';

-- ====================================================================================
-- 4. ADD VERSION CONSTRAINT
-- ====================================================================================
-- Ensure version is never negative (optimistic locking integrity)
ALTER TABLE accounts
ADD CONSTRAINT chk_version_positive
CHECK (version >= 0);

COMMENT ON CONSTRAINT chk_version_positive ON accounts IS
'Optimistic locking version must be non-negative';

-- ====================================================================================
-- 5. ADD TABLE AND COLUMN COMMENTS
-- ====================================================================================
-- Document the accounts table
COMMENT ON TABLE accounts IS
'User financial accounts including bank accounts, credit cards, and investment accounts.
Uses soft deletes (deleted_at IS NULL = active) and optimistic locking (version column).
Balances stored in specified currency (default USD).';

COMMENT ON COLUMN accounts.id IS 'Primary key - auto-generated account ID';
COMMENT ON COLUMN accounts.user_id IS 'Owner of this account (FK to users table)';
COMMENT ON COLUMN accounts.name IS 'User-defined account name (e.g., "Chase Checking")';
COMMENT ON COLUMN accounts.type IS 'Account type code (FK to account_types lookup table)';
COMMENT ON COLUMN accounts.current_balance IS
'Current account balance in specified currency. For credit cards: negative = debt owed, positive = overpayment/credit.';
COMMENT ON COLUMN accounts.currency_code IS 'ISO 4217 currency code (FK to currencies table, default USD)';
COMMENT ON COLUMN accounts.bank_name IS 'Optional: Financial institution name';
COMMENT ON COLUMN accounts.version IS
'Optimistic locking version counter. Application must increment on each update and verify unchanged during UPDATE.';
COMMENT ON COLUMN accounts.deleted_at IS
'Soft delete timestamp. NULL = active account, non-NULL = deleted (preserved for historical transaction integrity).';
