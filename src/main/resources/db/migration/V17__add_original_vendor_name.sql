-- Add original_vendor_name to transactions
ALTER TABLE transactions ADD COLUMN original_vendor_name VARCHAR(255);

-- Backfill existing records: original_vendor_name defaults to description
UPDATE transactions SET original_vendor_name = description;
