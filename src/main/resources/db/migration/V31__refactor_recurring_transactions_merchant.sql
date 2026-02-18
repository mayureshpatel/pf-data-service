-- V31: Refactor recurring_transactions table
-- Changes:
--   - merchant_name (VARCHAR, NOT NULL) replaced by merchant_id (FK → merchants, NOT NULL)

-- ====================================================================================
-- 1. ADD merchant_id COLUMN (nullable during migration)
-- ====================================================================================
ALTER TABLE recurring_transactions ADD COLUMN merchant_id BIGINT;

-- ====================================================================================
-- 2. SEED MERCHANTS FROM EXISTING merchant_name DATA
--    Insert any distinct merchant_name not already present as a global merchant.
-- ====================================================================================
INSERT INTO merchants (original_name, clean_name)
SELECT DISTINCT merchant_name, merchant_name
FROM recurring_transactions
WHERE merchant_name IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM merchants m
      WHERE m.original_name = recurring_transactions.merchant_name
        AND m.user_id IS NULL
  );

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
    FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE SET NULL;

CREATE INDEX idx_recurring_transactions_merchant_id ON recurring_transactions(merchant_id);

-- ====================================================================================
-- 5. ENFORCE NOT NULL (merchant_name was NOT NULL; merchant_id should be too)
-- ====================================================================================
ALTER TABLE recurring_transactions
    ALTER COLUMN merchant_id SET NOT NULL;

-- ====================================================================================
-- 6. DROP merchant_name (replaced by merchant_id)
-- ====================================================================================
ALTER TABLE recurring_transactions DROP COLUMN merchant_name;
