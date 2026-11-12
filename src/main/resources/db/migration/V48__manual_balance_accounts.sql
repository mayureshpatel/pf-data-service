-- V48: Manual-balance accounts, LOAN account type, contribution/earnings transaction types
-- Purpose: some accounts (investment, retirement, loans) have a balance entered by hand rather
--          than derived from transactions.

-- accounts.balance_mode: DERIVED (default, balance follows transactions) or MANUAL (hand-entered)
ALTER TABLE accounts
    ADD COLUMN balance_mode VARCHAR(10) NOT NULL DEFAULT 'DERIVED';

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_balance_mode
        CHECK (balance_mode IN ('DERIVED', 'MANUAL'));

-- EMPLOYER_CONTRIBUTION is 21 characters, so the type column (VARCHAR(20)) is widened too
ALTER TABLE transactions
    ALTER COLUMN type TYPE VARCHAR(30);

-- widen the transaction type check (same constraint name and approach as V25)
ALTER TABLE transactions
    DROP CONSTRAINT chk_transaction_type;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT',
                        'CONTRIBUTION', 'EMPLOYER_CONTRIBUTION', 'EARNINGS'));

-- LOAN account type: a liability, styled like CREDIT_CARD
INSERT INTO account_types (code, label, icon, color, is_asset, sort_order)
VALUES ('LOAN', 'Loan', 'pi-credit-card', 'text-purple-600', false,
        (SELECT COALESCE(MAX(sort_order), 0) + 1 FROM account_types))
ON CONFLICT (code) DO NOTHING;
