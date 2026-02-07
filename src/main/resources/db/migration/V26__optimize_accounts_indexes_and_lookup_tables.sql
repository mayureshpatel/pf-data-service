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
CREATE TABLE account_types (
    code        VARCHAR(20) PRIMARY KEY,
    label       VARCHAR(50) NOT NULL,
    icon        VARCHAR(50),
    color       VARCHAR(20),
    is_asset    BOOLEAN NOT NULL,  -- true for checking/savings/investment/cash, false for credit_card
    sort_order  INTEGER NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial account types with frontend metadata
INSERT INTO account_types (code, label, icon, color, is_asset, sort_order) VALUES
    ('CHECKING',    'Checking',    'pi-wallet',      'text-blue-600',   true,  1),
    ('SAVINGS',     'Savings',     'pi-piggy-bank',  'text-green-600',  true,  2),
    ('CREDIT_CARD', 'Credit Card', 'pi-credit-card', 'text-purple-600', false, 3),
    ('INVESTMENT',  'Investment',  'pi-chart-line',  'text-orange-600', true,  4),
    ('CASH',        'Cash',        'pi-money-bill',  'text-gray-600',   true,  5);

-- Add foreign key constraint (validates existing data first)
ALTER TABLE accounts
ADD CONSTRAINT fk_accounts_type
FOREIGN KEY (type) REFERENCES account_types(code);

-- Remove old check constraint (now redundant)
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS chk_account_type;

-- Add comment
COMMENT ON TABLE account_types IS
'Lookup table for account types with metadata (icons, colors, labels). Used by frontend for display configuration.';

-- ====================================================================================
-- 4. CREATE CURRENCIES LOOKUP TABLE
-- ====================================================================================
-- Create lookup table for currency codes
CREATE TABLE currencies (
    code   CHAR(3) PRIMARY KEY,
    name   VARCHAR(50) NOT NULL,
    symbol VARCHAR(5) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed common currencies
INSERT INTO currencies (code, name, symbol) VALUES
    ('USD', 'US Dollar', '$'),
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
FOREIGN KEY (currency_code) REFERENCES currencies(code);

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
