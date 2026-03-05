-- Add soft delete support

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE accounts
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE transactions
    ADD COLUMN deleted_at TIMESTAMP;

-- Index for performance when filtering
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
CREATE INDEX idx_accounts_deleted_at ON accounts (deleted_at);
CREATE INDEX idx_transactions_deleted_at ON transactions (deleted_at);
