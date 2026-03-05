-- Optimize dashboard aggregation queries
CREATE INDEX idx_transactions_account_date ON transactions (account_id, date);

-- Optimize account lookups by user (e.g. ownership checks)
CREATE INDEX idx_accounts_user ON accounts (user_id);

-- Optimize category lookups
CREATE INDEX idx_categories_user ON categories (user_id);
