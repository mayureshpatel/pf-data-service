-- V30: Refactor transactions table
-- Changes:
--   - vendor_name (VARCHAR) replaced by merchant_id (FK → merchants)
--   - original_vendor_name dropped (original_name now lives on the merchants table)
--   - date column converted from DATE → TIMESTAMPTZ
--   - post_date column converted from DATE → TIMESTAMPTZ

-- ====================================================================================
-- 1. ADD merchant_id COLUMN
-- ====================================================================================
ALTER TABLE transactions ADD COLUMN merchant_id BIGINT;

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
  AND NOT EXISTS (
      SELECT 1 FROM merchants m
      WHERE m.original_name = transactions.vendor_name
        AND m.user_id IS NULL
  );

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
    FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);

-- ====================================================================================
-- 5. DROP vendor_name (replaced by merchant_id)
-- ====================================================================================
ALTER TABLE transactions DROP COLUMN vendor_name;

-- ====================================================================================
-- 6. DROP original_vendor_name (original_name now lives on merchants table)
-- ====================================================================================
ALTER TABLE transactions DROP COLUMN original_vendor_name;

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
