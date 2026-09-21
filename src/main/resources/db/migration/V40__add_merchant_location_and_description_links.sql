-- V40: Deliberate merchant identity, phase 1 (PF-844, PF-EPIC-048)
-- Purpose: additive-only groundwork for replacing original_name/clean_name identity with a
--          user-typed name + optional location, and a separate table that remembers which raw
--          transaction descriptions map to which merchant. Nothing is dropped or renamed here --
--          existing code reading original_name/clean_name keeps working unchanged. The old
--          columns are dropped later, in V42, only once the repository/service layer (PF-845) no
--          longer references them.

-- ====================================================================================
-- 1. ADD NEW MERCHANT IDENTITY/LOCATION COLUMNS (nullable -- backfilled by V41)
-- ====================================================================================
ALTER TABLE merchants ADD COLUMN name VARCHAR(255);
ALTER TABLE merchants ADD COLUMN city VARCHAR(120);
ALTER TABLE merchants ADD COLUMN state VARCHAR(120);
ALTER TABLE merchants ADD COLUMN postal_code VARCHAR(20);
ALTER TABLE merchants ADD COLUMN country VARCHAR(60);

COMMENT ON COLUMN merchants.name IS
    'User-typed merchant name -- replaces original_name/clean_name as identity. Backfilled by V41, made NOT NULL by V42.';

-- ====================================================================================
-- 2. CREATE MERCHANT_DESCRIPTION_LINKS TABLE
-- ====================================================================================
CREATE TABLE merchant_description_links
(
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant_id            BIGINT       NOT NULL REFERENCES merchants (id) ON DELETE CASCADE,
    description             VARCHAR(255) NOT NULL,
    normalized_description VARCHAR(255) NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_merchant_description_links_user_normalized
    ON merchant_description_links (user_id, normalized_description);

CREATE INDEX idx_merchant_description_links_merchant_id ON merchant_description_links (merchant_id);

COMMENT ON TABLE merchant_description_links IS
    'Remembers which raw transaction descriptions a user has linked to which merchant, captured automatically on assignment and manageable explicitly. CSV import matches against this table only -- it never creates a merchant.';
COMMENT ON COLUMN merchant_description_links.description IS 'Raw text as linked, e.g. from transactions.description.';
COMMENT ON COLUMN merchant_description_links.normalized_description IS 'Lookup key: trim + casefold + whitespace-collapse of description.';
