-- Enable pg_trgm extension for efficient text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Optimize category-based reporting (e.g. "Spending by Category")
CREATE INDEX idx_transactions_category_id ON transactions (category_id);

-- Optimize type-based filtering (e.g. "Income vs Expense")
CREATE INDEX idx_transactions_type ON transactions (type);

-- Optimize text search (e.g. "Find all transactions matching 'Uber'")
-- GIN index with gin_trgm_ops is highly efficient for LIKE '%term%' queries
CREATE INDEX idx_transactions_description_trgm ON transactions USING GIN (description gin_trgm_ops);
