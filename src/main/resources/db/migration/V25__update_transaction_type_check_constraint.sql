-- Drop the existing check constraint
ALTER TABLE transactions DROP CONSTRAINT chk_transaction_type;

-- Re-add the check constraint with the new ADJUSTMENT type
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_type 
CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT'));
