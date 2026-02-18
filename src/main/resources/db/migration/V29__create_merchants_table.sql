-- V29: Create merchants table
-- Purpose: Normalize merchant/vendor data with original_name (raw bank text) and clean_name (display name).
--          Replaces the vendor_rules keyword-matching approach with a direct name-to-merchant lookup.
--          vendor_rules is dropped here as its role is superseded by merchants.

-- ====================================================================================
-- 1. CREATE MERCHANTS TABLE
-- ====================================================================================
CREATE TABLE merchants (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users(id) ON DELETE CASCADE, -- NULL = global/system merchant
    original_name VARCHAR(255) NOT NULL,
    clean_name    VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Unique global merchants by original_name (user_id IS NULL)
CREATE UNIQUE INDEX idx_merchants_global_original_name
    ON merchants(original_name)
    WHERE user_id IS NULL;

-- Unique user-scoped merchants by original_name per user
CREATE UNIQUE INDEX idx_merchants_user_original_name
    ON merchants(user_id, original_name)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_merchants_user_id ON merchants(user_id);

COMMENT ON TABLE merchants IS
    'Merchant registry mapping raw bank descriptions (original_name) to normalized display names (clean_name). '
    'Global merchants have user_id = NULL; user-scoped overrides have a specific user_id.';

COMMENT ON COLUMN merchants.original_name IS 'Raw text as it appears in the bank/CSV import (e.g. "WHOLEFDS #12345").';
COMMENT ON COLUMN merchants.clean_name    IS 'Normalized display name shown in the UI (e.g. "Whole Foods").';

-- ====================================================================================
-- 2. MIGRATE VENDOR RULES → MERCHANTS
-- ====================================================================================
-- vendor_rules used keyword + vendor_name; we treat keyword as original_name and vendor_name as clean_name.
INSERT INTO merchants (user_id, original_name, clean_name, created_at, updated_at)
SELECT
    user_id,
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
