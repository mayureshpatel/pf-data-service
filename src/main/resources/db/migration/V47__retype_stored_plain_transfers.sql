-- V47: Retype stored plain TRANSFER rows (PF-936)
-- Purpose: plain TRANSFER is a filter-only value; a stored transfer must carry a direction
--          (TRANSFER_IN or TRANSFER_OUT). The transaction form used to let a plain TRANSFER be
--          saved, and the service now rejects it. Balance logic has always counted a plain
--          TRANSFER as money out, so TRANSFER_OUT keeps every account balance unchanged.
UPDATE transactions
SET type = 'TRANSFER_OUT'
WHERE type = 'TRANSFER';
