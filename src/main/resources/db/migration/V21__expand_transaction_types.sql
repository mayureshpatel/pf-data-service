-- Update transaction type constraint to include new transfer directions
ALTER TABLE transactions DROP CONSTRAINT chk_transaction_type;
ALTER TABLE transactions ADD CONSTRAINT chk_transaction_type 
    CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'TRANSFER_IN', 'TRANSFER_OUT'));
