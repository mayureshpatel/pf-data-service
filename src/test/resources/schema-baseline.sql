-- 1. Create Users Table
CREATE TABLE users
(
    id                     BIGSERIAL PRIMARY KEY,
    username               VARCHAR(50)  NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    email                  VARCHAR(100) NOT NULL UNIQUE,
    role                   VARCHAR(20)  NOT NULL DEFAULT 'USER',
    last_updated_by        VARCHAR(255) NOT NULL,
    last_updated_timestamp TIMESTAMP,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Categories Table
CREATE TABLE categories
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(20),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. Create Accounts Table
CREATE TABLE accounts
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    type            VARCHAR(20)    NOT NULL, -- CHECKING, SAVINGS, etc.
    current_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 4. Create Transactions Table
CREATE TABLE transactions
(
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT         NOT NULL,
    category_id BIGINT,                  -- Nullable, because a transaction might be uncategorized initially
    amount      NUMERIC(19, 2) NOT NULL,
    date        DATE           NOT NULL,
    description VARCHAR(255),
    type        VARCHAR(20)    NOT NULL, -- CREDIT/DEBIT
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

-- 5. Create Indexes for Performance
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_date ON transactions (date);-- Enable pg_trgm extension for efficient text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Optimize category-based reporting (e.g. "Spending by Category")
CREATE INDEX idx_transactions_category_id ON transactions (category_id);

-- Optimize type-based filtering (e.g. "Income vs Expense")
CREATE INDEX idx_transactions_type ON transactions (type);

-- Optimize text search (e.g. "Find all transactions matching 'Uber'")
-- GIN index with gin_trgm_ops is highly efficient for LIKE '%term%' queries
CREATE INDEX idx_transactions_description_trgm ON transactions USING GIN (description gin_trgm_ops);
CREATE TABLE account_snapshots
(
    id            BIGSERIAL PRIMARY KEY,
    account_id    BIGINT         NOT NULL,
    snapshot_date DATE           NOT NULL,
    balance       NUMERIC(19, 2) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_snapshots_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT uq_account_snapshot_date UNIQUE (account_id, snapshot_date)
);

CREATE INDEX idx_snapshots_account_date ON account_snapshots (account_id, snapshot_date);
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
-- Seed Initial User (Username: admin, Password: password)
INSERT INTO users (username, password_hash, email, role, last_updated_by, last_updated_timestamp)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'admin@example.com', 'ADMIN', 'system',
        CURRENT_TIMESTAMP);

-- Seed Categories for admin
INSERT INTO categories (user_id, name, color)
SELECT id, 'Groceries', '#4CAF50'
FROM users
WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Dining Out', '#FF9800'
FROM users
WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Utilities', '#2196F3'
FROM users
WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Entertainment', '#9C27B0'
FROM users
WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Transportation', '#F44336'
FROM users
WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Gas', '#795548'
FROM users
WHERE username = 'admin';

-- Seed Accounts for admin
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'Main Checking', 'CHECKING', 5000.00, 'USD'
FROM users
WHERE username = 'admin';
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'High Yield Savings', 'SAVINGS', 15000.00, 'USD'
FROM users
WHERE username = 'admin';
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'Travel Card', 'CREDIT_CARD', -250.00, 'USD'
FROM users
WHERE username = 'admin';
-- V14: Robust Seed Data (REVERTED TO PRE-MODIFICATION STATE FOR CHECKSUM)
-- 1. Seed Initial User (Username: admin, Password: password)
INSERT INTO users (username, password_hash, email, role, last_updated_by, last_updated_timestamp)
SELECT 'admin',
       '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2',
       'admin@example.com',
       'ADMIN',
       'system',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 2. Seed Categories & Sub-categories for admin
-- Parent Categories
INSERT INTO categories (user_id, name, color)
SELECT (SELECT id FROM users WHERE username = 'admin' LIMIT 1), name, color
FROM (VALUES ('Housing', '#2196F3'),
             ('Food', '#4CAF50'),
             ('Transportation', '#F44336'),
             ('Entertainment', '#9C27B0'),
             ('Income', '#FFD700'),
             ('Utilities', '#00BCD4')) AS t(name, color)
WHERE NOT EXISTS (SELECT 1
                  FROM categories
                  WHERE name = t.name AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- Sub-categories
INSERT INTO categories (user_id, name, color, parent_id)
SELECT (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
       name,
       color,
       (SELECT id
        FROM categories
        WHERE name = p_name
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
          AND parent_id IS NULL
        LIMIT 1)
FROM (VALUES ('Rent', '#42A5F5', 'Housing'),
             ('Electricity', '#00ACC1', 'Utilities'),
             ('Water', '#26C6DA', 'Utilities'),
             ('Groceries', '#66BB6A', 'Food'),
             ('Dining Out', '#81C784', 'Food'),
             ('Gas', '#EF5350', 'Transportation'),
             ('Salary', '#FFF176', 'Income'),
             ('Streaming', '#BA68C8', 'Entertainment')) AS t(name, color, p_name)
WHERE NOT EXISTS (SELECT 1
                  FROM categories
                  WHERE name = t.name
                    AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
                    AND parent_id IS NOT NULL);

-- 3. Seed Accounts for admin
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT (SELECT id FROM users WHERE username = 'admin' LIMIT 1), name, type, bal, 'USD'
FROM (VALUES ('Main Checking', 'CHECKING', 4250.00),
             ('High Yield Savings', 'SAVINGS', 15000.00),
             ('Travel Card', 'CREDIT_CARD', -120.50)) AS t(name, type, bal)
WHERE NOT EXISTS (SELECT 1
                  FROM accounts
                  WHERE name = t.name AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- 4. Seed 6 Months of Transactions (Aug 2025 - Jan 2026)
-- We'll delete existing transactions for these accounts to ensure a clean slate if V13 left crumbs
DELETE
FROM transactions
WHERE account_id IN (SELECT id FROM accounts WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- Salary (Semi-monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Main Checking'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Salary' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       3200.00,
       d::date,
       'Monthly Salary',
       'INCOME'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Rent (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Main Checking'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Rent' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       1800.00,
       d::date,
       'Monthly Rent Payment',
       'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Utilities (Variable)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Main Checking'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Electricity'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (80 + (random() * 40))::numeric(19, 2),
       (d + interval '10 days')::date,
       'Electric Bill',
       'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Groceries (Weekly-ish)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Main Checking'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Groceries'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (100 + (random() * 50))::numeric(19, 2),
       d::date,
       'Weekly Groceries',
       'EXPENSE'
FROM generate_series('2025-08-03'::date, '2026-01-15'::date, '7 days'::interval) d;

-- Dining Out (Randomized)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Main Checking'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Dining Out'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (25 + (random() * 60))::numeric(19, 2),
       d::date,
       'Dinner Out',
       'EXPENSE'
FROM generate_series('2025-08-05'::date, '2026-01-15'::date, '5 days'::interval) d;

-- Gas (Bi-weekly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Travel Card'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Gas' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (40 + (random() * 15))::numeric(19, 2),
       d::date,
       'Gas Station',
       'EXPENSE'
FROM generate_series('2025-08-02'::date, '2026-01-18'::date, '12 days'::interval) d;

-- Streaming (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id
        FROM accounts
        WHERE name = 'Travel Card'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       (SELECT id
        FROM categories
        WHERE name = 'Streaming'
          AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        LIMIT 1),
       15.99,
       (d + interval '4 days')::date,
       'Netflix Subscription',
       'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- One-time larger expenses
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
VALUES ((SELECT id
         FROM accounts
         WHERE name = 'Travel Card'
           AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
         LIMIT 1), (SELECT id
                    FROM categories
                    WHERE name = 'Entertainment'
                      AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
                    LIMIT 1), 450.00, '2025-11-20', 'Concert Tickets', 'EXPENSE'),
       ((SELECT id
         FROM accounts
         WHERE name = 'Main Checking'
           AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
         LIMIT 1), (SELECT id
                    FROM categories
                    WHERE name = 'Housing'
                      AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
                    LIMIT 1), 320.00, '2025-09-15', 'New Furniture', 'EXPENSE'),
       ((SELECT id
         FROM accounts
         WHERE name = 'Main Checking'
           AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
         LIMIT 1), (SELECT id
                    FROM categories
                    WHERE name = 'Income' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
                    LIMIT 1), 500.00, '2025-12-24', 'Holiday Bonus', 'INCOME');-- V15: Reset and Seed Robust Data
-- 1. Wipe everything and reset all sequences to 1
TRUNCATE TABLE transactions, transaction_tags, tags, category_rules, categories, account_snapshots, file_import_history, accounts, users RESTART IDENTITY CASCADE;

-- 2. Seed Initial User (Will get ID 1)
INSERT INTO users (username, password_hash, email, role, last_updated_by, last_updated_timestamp)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'admin@example.com', 'ADMIN', 'system',
        CURRENT_TIMESTAMP);

-- 3. Seed Categories (Will get IDs 1-6 for parents)
INSERT INTO categories (user_id, name, color)
VALUES (1, 'Housing', '#2196F3'),
       (1, 'Food', '#4CAF50'),
       (1, 'Transportation', '#F44336'),
       (1, 'Entertainment', '#9C27B0'),
       (1, 'Income', '#FFD700'),
       (1, 'Utilities', '#00BCD4');

-- Sub-categories
INSERT INTO categories (user_id, name, color, parent_id)
VALUES (1, 'Rent', '#42A5F5', (SELECT id FROM categories WHERE name = 'Housing' AND parent_id IS NULL LIMIT 1)),
       (1, 'Electricity', '#00ACC1',
        (SELECT id FROM categories WHERE name = 'Utilities' AND parent_id IS NULL LIMIT 1)),
       (1, 'Water', '#26C6DA', (SELECT id FROM categories WHERE name = 'Utilities' AND parent_id IS NULL LIMIT 1)),
       (1, 'Groceries', '#66BB6A', (SELECT id FROM categories WHERE name = 'Food' AND parent_id IS NULL LIMIT 1)),
       (1, 'Dining Out', '#81C784', (SELECT id FROM categories WHERE name = 'Food' AND parent_id IS NULL LIMIT 1)),
       (1, 'Gas', '#EF5350', (SELECT id FROM categories WHERE name = 'Transportation' AND parent_id IS NULL LIMIT 1)),
       (1, 'Salary', '#FFF176', (SELECT id FROM categories WHERE name = 'Income' AND parent_id IS NULL LIMIT 1)),
       (1, 'Streaming', '#BA68C8',
        (SELECT id FROM categories WHERE name = 'Entertainment' AND parent_id IS NULL LIMIT 1));

-- 4. Seed Accounts (Main Checking will be ID 1)
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
VALUES (1, 'Main Checking', 'CHECKING', 4250.00, 'USD'),
       (1, 'High Yield Savings', 'SAVINGS', 15000.00, 'USD'),
       (1, 'Travel Card', 'CREDIT_CARD', -120.50, 'USD');

-- 5. Seed 6 Months of Transactions (Aug 2025 - Jan 2026)
-- Salary (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 1, (SELECT id FROM categories WHERE name = 'Salary' LIMIT 1), 3200.00, d::date, 'Monthly Salary', 'INCOME'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Rent (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 1, (SELECT id FROM categories WHERE name = 'Rent' LIMIT 1), 1800.00, d::date, 'Monthly Rent Payment', 'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Utilities (Variable)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 1,
       (SELECT id FROM categories WHERE name = 'Electricity' LIMIT 1),
       (80 + (random() * 40))::numeric(19, 2),
       (d + interval '10 days')::date,
       'Electric Bill',
       'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Groceries (Weekly-ish)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 1,
       (SELECT id FROM categories WHERE name = 'Groceries' LIMIT 1),
       (100 + (random() * 50))::numeric(19, 2),
       d::date,
       'Weekly Groceries',
       'EXPENSE'
FROM generate_series('2025-08-03'::date, '2026-01-15'::date, '7 days'::interval) d;

-- Dining Out (Randomized)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 1,
       (SELECT id FROM categories WHERE name = 'Dining Out' LIMIT 1),
       (25 + (random() * 60))::numeric(19, 2),
       d::date,
       'Dinner Out',
       'EXPENSE'
FROM generate_series('2025-08-05'::date, '2026-01-15'::date, '5 days'::interval) d;

-- Gas (Bi-weekly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id FROM accounts WHERE name = 'Travel Card' LIMIT 1),
       (SELECT id FROM categories WHERE name = 'Gas' LIMIT 1),
       (40 + (random() * 15))::numeric(19, 2),
       d::date,
       'Gas Station',
       'EXPENSE'
FROM generate_series('2025-08-02'::date, '2026-01-18'::date, '12 days'::interval) d;

-- Streaming (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT (SELECT id FROM accounts WHERE name = 'Travel Card' LIMIT 1),
       (SELECT id FROM categories WHERE name = 'Streaming' LIMIT 1),
       15.99,
       (d + interval '4 days')::date,
       'Netflix Subscription',
       'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- One-time larger expenses
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
VALUES ((SELECT id FROM accounts WHERE name = 'Travel Card' LIMIT 1),
        (SELECT id FROM categories WHERE name = 'Entertainment' LIMIT 1), 450.00, '2025-11-20', 'Concert Tickets',
        'EXPENSE'),
       (1, (SELECT id FROM categories WHERE name = 'Housing' LIMIT 1), 320.00, '2025-09-15', 'New Furniture',
        'EXPENSE'),
       (1, (SELECT id FROM categories WHERE name = 'Income' LIMIT 1), 500.00, '2025-12-24', 'Holiday Bonus', 'INCOME');
-- Add vendor_name to transactions
ALTER TABLE transactions
    ADD COLUMN vendor_name VARCHAR(100);

-- Create vendor_rules table
CREATE TABLE vendor_rules
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    keyword     VARCHAR(255)        NOT NULL,
    vendor_name VARCHAR(100)        NOT NULL,
    priority    INT       DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT fk_vendor_rules_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_rules_user ON vendor_rules (user_id);
-- Add original_vendor_name to transactions
ALTER TABLE transactions
    ADD COLUMN original_vendor_name VARCHAR(255);

-- Backfill existing records: original_vendor_name defaults to description
UPDATE transactions
SET original_vendor_name = description;
CREATE TABLE budgets
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id),
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    amount      DECIMAL(19, 2) NOT NULL,
    month       INTEGER        NOT NULL CHECK (month >= 1 AND month <= 12),
    year        INTEGER        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    UNIQUE (user_id, category_id, month, year)
);

CREATE INDEX idx_budgets_user_month_year ON budgets (user_id, year, month);
CREATE TABLE recurring_transactions
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT         NOT NULL REFERENCES users (id),
    account_id    BIGINT REFERENCES accounts (id),
    merchant_name VARCHAR(255)   NOT NULL,
    amount        DECIMAL(19, 2) NOT NULL,
    frequency     VARCHAR(20)    NOT NULL,
    last_date     DATE,
    next_date     DATE           NOT NULL,
    active        BOOLEAN                  DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_recurring_user_next_date ON recurring_transactions (user_id, next_date);
CREATE TABLE file_import_history
(
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT       NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    file_hash         VARCHAR(64)  NOT NULL,
    transaction_count INT          NOT NULL,
    imported_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_import_account_hash ON file_import_history (account_id, file_hash);ALTER TABLE categories
    ADD COLUMN icon VARCHAR(50);
ALTER TABLE categories
    ADD COLUMN type VARCHAR(20) DEFAULT 'EXPENSE';

-- Update existing categories to have a default type
UPDATE categories
SET type = 'EXPENSE';
-- Update transaction type constraint to include new transfer directions
ALTER TABLE transactions
    DROP CONSTRAINT chk_transaction_type;
ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT'));
ALTER TABLE accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE accounts
    ADD COLUMN bank_name VARCHAR(50);
ALTER TABLE transactions
    ADD COLUMN post_date DATE;
-- Drop the existing check constraint
ALTER TABLE transactions
    DROP CONSTRAINT chk_transaction_type;

-- Re-add the check constraint with the new ADJUSTMENT type
ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT'));
-- V26: Index Optimization & Lookup Tables
-- Purpose: Fix critical performance issues and establish foundational lookup tables

-- ====================================================================================
-- 1. REMOVE DUPLICATE INDEX
-- ====================================================================================
-- Problem: idx_accounts_user and idx_accounts_user_id are duplicates
-- Solution: Drop idx_accounts_user (keep the more descriptive name)
DROP INDEX IF EXISTS idx_accounts_user;

-- ====================================================================================
-- 2. OPTIMIZE SOFT DELETE INDEX
-- ====================================================================================
-- Problem: Full index on deleted_at is inefficient (most queries filter WHERE deleted_at IS NULL)
-- Solution: Replace with partial indexes

-- Drop inefficient full index
DROP INDEX IF EXISTS idx_accounts_deleted_at;

-- Create partial index for active accounts (90%+ of queries)
CREATE INDEX idx_accounts_active
    ON accounts (user_id, type, current_balance)
    WHERE deleted_at IS NULL;

-- Create partial index for deleted accounts (if queried separately)
CREATE INDEX idx_accounts_deleted
    ON accounts (deleted_at, user_id)
    WHERE deleted_at IS NOT NULL;

-- ====================================================================================
-- 3. CREATE ACCOUNT TYPES LOOKUP TABLE
-- ====================================================================================
-- Create lookup table for account type metadata
CREATE TABLE account_types
(
    code       VARCHAR(20) PRIMARY KEY,
    label      VARCHAR(50) NOT NULL,
    icon       VARCHAR(50),
    color      VARCHAR(20),
    is_asset   BOOLEAN     NOT NULL, -- true for checking/savings/investment/cash, false for credit_card
    sort_order INTEGER     NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial account types with frontend metadata
INSERT INTO account_types (code, label, icon, color, is_asset, sort_order)
VALUES ('CHECKING', 'Checking', 'pi-wallet', 'text-blue-600', true, 1),
       ('SAVINGS', 'Savings', 'pi-piggy-bank', 'text-green-600', true, 2),
       ('CREDIT_CARD', 'Credit Card', 'pi-credit-card', 'text-purple-600', false, 3),
       ('INVESTMENT', 'Investment', 'pi-chart-line', 'text-orange-600', true, 4),
       ('CASH', 'Cash', 'pi-money-bill', 'text-gray-600', true, 5);

-- Add foreign key constraint (validates existing data first)
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_type
        FOREIGN KEY (type) REFERENCES account_types (code);

-- Remove old check constraint (now redundant)
ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS chk_account_type;

-- Add comment
COMMENT ON TABLE account_types IS
    'Lookup table for account types with metadata (icons, colors, labels). Used by frontend for display configuration.';

-- ====================================================================================
-- 4. CREATE CURRENCIES LOOKUP TABLE
-- ====================================================================================
-- Create lookup table for currency codes
CREATE TABLE currencies
(
    code       CHAR(3) PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    symbol     VARCHAR(5)  NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed common currencies
INSERT INTO currencies (code, name, symbol)
VALUES ('USD', 'US Dollar', '$'),
       ('EUR', 'Euro', '€'),
       ('GBP', 'British Pound', '£'),
       ('CAD', 'Canadian Dollar', 'CA$'),
       ('JPY', 'Japanese Yen', '¥'),
       ('AUD', 'Australian Dollar', 'A$'),
       ('CHF', 'Swiss Franc', 'CHF'),
       ('INR', 'Indian Rupee', '₹');

-- Add foreign key constraint (validates existing data - should all be USD)
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_currency
        FOREIGN KEY (currency_code) REFERENCES currencies (code);

-- Add comment
COMMENT ON TABLE currencies IS
    'ISO 4217 currency codes with display metadata. Validates currency_code in accounts table.';

-- ====================================================================================
-- 5. ADD PERFORMANCE INDEX FOR TYPE QUERIES
-- ====================================================================================
-- Index for queries filtering by account type
CREATE INDEX idx_accounts_type
    ON accounts (type)
    WHERE deleted_at IS NULL;
-- V27: Data Integrity Constraints & Audit Columns
-- Purpose: Add audit trail and business constraints to prevent data quality issues

-- ====================================================================================
-- 1. ADD AUDIT COLUMNS
-- ====================================================================================
-- Add created_by, updated_by, deleted_by for audit trail
ALTER TABLE accounts
    ADD COLUMN created_by BIGINT REFERENCES users (id),
    ADD COLUMN updated_by BIGINT REFERENCES users (id),
    ADD COLUMN deleted_by BIGINT REFERENCES users (id);

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
-- V28: Timestamp Timezone Upgrade
-- Purpose: Upgrade timestamp columns to timestamptz for proper timezone handling

-- ====================================================================================
-- 1. UPGRADE TIMESTAMP COLUMNS TO TIMESTAMPTZ
-- ====================================================================================
-- Convert all timestamp columns to timestamptz (storing in UTC)
ALTER TABLE accounts
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

-- Update default values to use timezone-aware NOW()
ALTER TABLE accounts
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

COMMENT ON COLUMN accounts.created_at IS
    'Timestamp when account was created (stored in UTC, displayed in user timezone)';
COMMENT ON COLUMN accounts.updated_at IS
    'Timestamp when account was last updated (stored in UTC, auto-updated by trigger)';
COMMENT ON COLUMN accounts.deleted_at IS
    'Timestamp when account was soft-deleted (stored in UTC)';

-- ====================================================================================
-- 2. CREATE TRIGGER FOR AUTO-UPDATE TIMESTAMP
-- ====================================================================================
-- Create function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE
    ON accounts
    FOR EACH ROW
EXECUTE FUNCTION update_accounts_updated_at();

COMMENT ON FUNCTION update_accounts_updated_at() IS
    'Automatically updates the updated_at column to current timestamp on every UPDATE';

-- ====================================================================================
-- 3. UPGRADE LOOKUP TABLE TIMESTAMPS
-- ====================================================================================
-- Upgrade account_types table
ALTER TABLE account_types
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE account_types
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

-- Create trigger for account_types
CREATE TRIGGER trg_account_types_updated_at
    BEFORE UPDATE
    ON account_types
    FOR EACH ROW
EXECUTE FUNCTION update_accounts_updated_at();

-- Upgrade currencies table
ALTER TABLE currencies
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE currencies
    ALTER COLUMN created_at SET DEFAULT NOW();
-- V29: Create merchants table
-- Purpose: Normalize merchant/vendor data with original_name (raw bank text) and clean_name (display name).
--          Replaces the vendor_rules keyword-matching approach with a direct name-to-merchant lookup.
--          vendor_rules is dropped here as its role is superseded by merchants.

-- ====================================================================================
-- 1. CREATE MERCHANTS TABLE
-- ====================================================================================
CREATE TABLE merchants
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id) ON DELETE CASCADE, -- NULL = global/system merchant
    original_name VARCHAR(255) NOT NULL,
    clean_name    VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Unique global merchants by original_name (user_id IS NULL)
CREATE UNIQUE INDEX idx_merchants_global_original_name
    ON merchants (original_name)
    WHERE user_id IS NULL;

-- Unique user-scoped merchants by original_name per user
CREATE UNIQUE INDEX idx_merchants_user_original_name
    ON merchants (user_id, original_name)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_merchants_user_id ON merchants (user_id);

COMMENT ON TABLE merchants IS
    'Merchant registry mapping raw bank descriptions (original_name) to normalized display names (clean_name). '
        'Global merchants have user_id = NULL; user-scoped overrides have a specific user_id.';

COMMENT ON COLUMN merchants.original_name IS 'Raw text as it appears in the bank/CSV import (e.g. "WHOLEFDS #12345").';
COMMENT ON COLUMN merchants.clean_name IS 'Normalized display name shown in the UI (e.g. "Whole Foods").';

-- ====================================================================================
-- 2. MIGRATE VENDOR RULES → MERCHANTS
-- ====================================================================================
-- vendor_rules used keyword + vendor_name; we treat keyword as original_name and vendor_name as clean_name.
INSERT INTO merchants (user_id, original_name, clean_name, created_at, updated_at)
SELECT user_id,
       keyword,
       vendor_name,
       COALESCE(created_at, NOW()),
       COALESCE(updated_at, NOW())
FROM vendor_rules;

-- ====================================================================================
-- 3. DROP VENDOR RULES TABLE (superseded by merchants)
-- ====================================================================================
DROP INDEX IF EXISTS idx_vendor_rules_user;
DROP TABLE vendor_rules;
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
        CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'CASH'));-- V30: Refactor transactions table
-- Changes:
--   - vendor_name (VARCHAR) replaced by merchant_id (FK → merchants)
--   - original_vendor_name dropped (original_name now lives on the merchants table)
--   - date column converted from DATE → TIMESTAMPTZ
--   - post_date column converted from DATE → TIMESTAMPTZ

-- ====================================================================================
-- 1. ADD merchant_id COLUMN
-- ====================================================================================
ALTER TABLE transactions
    ADD COLUMN merchant_id BIGINT;

-- ====================================================================================
-- 2. SEED MERCHANTS FROM EXISTING TRANSACTION vendor_name DATA
--    Insert any distinct vendor_name not already present as a global merchant.
--    We treat the existing vendor_name as both original_name and clean_name since
--    it was already a resolved value (not a raw bank string in all cases).
-- ====================================================================================
INSERT INTO merchants (original_name, clean_name)
SELECT DISTINCT vendor_name, vendor_name
FROM transactions
WHERE vendor_name IS NOT NULL
  AND NOT EXISTS (SELECT 1
                  FROM merchants m
                  WHERE m.original_name = transactions.vendor_name
                    AND m.user_id IS NULL);

-- ====================================================================================
-- 3. LINK TRANSACTIONS TO MERCHANTS
-- ====================================================================================
UPDATE transactions t
SET merchant_id = m.id
FROM merchants m
WHERE t.vendor_name = m.original_name
  AND m.user_id IS NULL;

-- ====================================================================================
-- 4. ADD FOREIGN KEY CONSTRAINT
-- ====================================================================================
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_merchant_id ON transactions (merchant_id);

-- ====================================================================================
-- 5. DROP vendor_name (replaced by merchant_id)
-- ====================================================================================
ALTER TABLE transactions
    DROP COLUMN vendor_name;

-- ====================================================================================
-- 6. DROP original_vendor_name (original_name now lives on merchants table)
-- ====================================================================================
ALTER TABLE transactions
    DROP COLUMN original_vendor_name;

-- ====================================================================================
-- 7. CONVERT date FROM DATE → TIMESTAMPTZ
--    Existing DATE values are interpreted as midnight UTC.
-- ====================================================================================
ALTER TABLE transactions
    ALTER COLUMN date TYPE TIMESTAMPTZ
        USING date::TIMESTAMP AT TIME ZONE 'UTC';

-- ====================================================================================
-- 8. CONVERT post_date FROM DATE → TIMESTAMPTZ
--    Existing DATE values are interpreted as midnight UTC.
-- ====================================================================================
ALTER TABLE transactions
    ALTER COLUMN post_date TYPE TIMESTAMPTZ
        USING post_date::TIMESTAMP AT TIME ZONE 'UTC';
-- V31: Refactor recurring_transactions table
-- Changes:
--   - merchant_name (VARCHAR, NOT NULL) replaced by merchant_id (FK → merchants, NOT NULL)

-- ====================================================================================
-- 1. ADD merchant_id COLUMN (nullable during migration)
-- ====================================================================================
ALTER TABLE recurring_transactions
    ADD COLUMN merchant_id BIGINT;

-- ====================================================================================
-- 2. SEED MERCHANTS FROM EXISTING merchant_name DATA
--    Insert any distinct merchant_name not already present as a global merchant.
-- ====================================================================================
INSERT INTO merchants (original_name, clean_name)
SELECT DISTINCT merchant_name, merchant_name
FROM recurring_transactions
WHERE merchant_name IS NOT NULL
  AND NOT EXISTS (SELECT 1
                  FROM merchants m
                  WHERE m.original_name = recurring_transactions.merchant_name
                    AND m.user_id IS NULL);

-- ====================================================================================
-- 3. LINK RECURRING TRANSACTIONS TO MERCHANTS
-- ====================================================================================
UPDATE recurring_transactions rt
SET merchant_id = m.id
FROM merchants m
WHERE rt.merchant_name = m.original_name
  AND m.user_id IS NULL;

-- ====================================================================================
-- 4. ADD FOREIGN KEY CONSTRAINT
-- ====================================================================================
ALTER TABLE recurring_transactions
    ADD CONSTRAINT fk_recurring_transactions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE SET NULL;

CREATE INDEX idx_recurring_transactions_merchant_id ON recurring_transactions (merchant_id);

-- ====================================================================================
-- 5. ENFORCE NOT NULL (merchant_name was NOT NULL; merchant_id should be too)
-- ====================================================================================
ALTER TABLE recurring_transactions
    ALTER COLUMN merchant_id SET NOT NULL;

-- ====================================================================================
-- 6. DROP merchant_name (replaced by merchant_id)
-- ====================================================================================
ALTER TABLE recurring_transactions
    DROP COLUMN merchant_name;
-- V32: Refactor category_rules table
-- Changes:
--   - category_name (VARCHAR) replaced by category_id (FK → categories)
--
-- Note: categories are user-scoped (every category row has a user_id).
--       Global rules (category_rules.user_id IS NULL) were seeded with category names
--       like 'Groceries', 'Dining Out', etc. but have no corresponding row in categories,
--       so their category_id will remain NULL after migration.
--       User-specific rules are matched by (user_id, name).

-- ====================================================================================
-- 1. ADD category_id COLUMN (nullable — global rules cannot resolve to a category row)
-- ====================================================================================
ALTER TABLE category_rules
    ADD COLUMN category_id BIGINT;

-- ====================================================================================
-- 2. BEST-EFFORT MATCH FOR USER-SPECIFIC RULES
--    Matches on (user_id, category_name = categories.name)
-- ====================================================================================
UPDATE category_rules cr
SET category_id = c.id
FROM categories c
WHERE cr.user_id IS NOT NULL
  AND cr.user_id = c.user_id
  AND cr.category_name = c.name;

-- ====================================================================================
-- 3. ADD FOREIGN KEY CONSTRAINT
-- ====================================================================================
ALTER TABLE category_rules
    ADD CONSTRAINT fk_category_rules_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL;

CREATE INDEX idx_category_rules_category_id ON category_rules (category_id);

-- ====================================================================================
-- 4. DROP category_name (replaced by category_id)
-- ====================================================================================
ALTER TABLE category_rules
    DROP COLUMN category_name;
-- 5. Add Sub-Category Support
ALTER TABLE categories
    ADD COLUMN parent_id BIGINT;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE CASCADE;-- 6. Add Tagging Support (Many-to-Many)
CREATE TABLE tags
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(20),                                -- Optional: UI color for the tag pill
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_tags_name_user UNIQUE (user_id, name) -- Prevent duplicate tag names for same user
);

CREATE TABLE transaction_tags
(
    transaction_id BIGINT NOT NULL,
    tag_id         BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_tt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_tt_tag_id ON transaction_tags (tag_id);CREATE TABLE category_rules
(
    id            BIGSERIAL PRIMARY KEY,
    keyword       VARCHAR(255) NOT NULL,
    category_name VARCHAR(50)  NOT NULL,
    priority      INT DEFAULT 0
);

-- Seed initial rules
INSERT INTO category_rules (keyword, category_name, priority)
VALUES ('PUBLIX', 'Groceries', 1),
       ('KROGER', 'Groceries', 1),
       ('WHOLE FOODS', 'Groceries', 1),
       ('TRADER JOE', 'Groceries', 1),
       ('WALMART', 'Groceries', 1),
       ('WEGMANS', 'Groceries', 1),
       ('MCDONALD', 'Dining Out', 1),
       ('STARBUCKS', 'Dining Out', 1),
       ('DUNKIN', 'Dining Out', 1),
       ('TACO BELL', 'Dining Out', 1),
       ('CHIPOTLE', 'Dining Out', 1),
       ('UBER EATS', 'Dining Out', 5),
       ('DOMINO', 'Dining Out', 1),
       ('AT&T', 'Utilities', 1),
       ('VERIZON', 'Utilities', 1),
       ('XFINITY', 'Utilities', 1),
       ('POWER', 'Utilities', 1),
       ('WATER', 'Utilities', 1),
       ('NETFLIX', 'Entertainment', 1),
       ('SPOTIFY', 'Entertainment', 1),
       ('STEAM', 'Entertainment', 1),
       ('NINTENDO', 'Entertainment', 1),
       ('UBER', 'Transportation', 1),
       ('LYFT', 'Transportation', 1),
       ('SHELL', 'Gas', 1),
       ('CHEVRON', 'Gas', 1),
       ('EXXON', 'Gas', 1);
-- Optimize dashboard aggregation queries
CREATE INDEX idx_transactions_account_date ON transactions (account_id, date);

-- Optimize account lookups by user (e.g. ownership checks)
CREATE INDEX idx_accounts_user ON accounts (user_id);

-- Optimize category lookups
CREATE INDEX idx_categories_user ON categories (user_id);
-- Add user_id to category_rules to support personalized rules
-- If user_id is NULL, the rule is considered "Global" (system default)

ALTER TABLE category_rules
    ADD COLUMN user_id BIGINT;

ALTER TABLE category_rules
    ADD CONSTRAINT fk_rules_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_rules_user ON category_rules (user_id);
-- Standardize auditing columns across all major tables

-- 1. Categories
ALTER TABLE categories
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. Tags
ALTER TABLE tags
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 3. Category Rules
ALTER TABLE category_rules
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
