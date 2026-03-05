-- 1. Add Safety: Prevent accidental history loss
ALTER TABLE transactions
    DROP CONSTRAINT fk_transactions_account,
    ADD CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT;

-- 2. Add Auditability
ALTER TABLE accounts
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE transactions
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 3. Add Currency Support (Future proofing)
ALTER TABLE accounts
    ADD COLUMN currency_code CHAR(3) NOT NULL DEFAULT 'USD';

-- 4. Add Data Integrity Checks
ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'));

ALTER TABLE accounts
    ADD CONSTRAINT chk_account_type
        CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'CASH'));