-- idx_transactions_account_id (account_id) is fully dominated by idx_transactions_account_date
-- (account_id, date), added later in V7 -- any query the single-column index could serve, the
-- composite index serves equally well since account_id is its leading column. Confirmed via
-- EXPLAIN ANALYZE against a realistic multi-user dataset that nothing relies on the single-column
-- index specifically. Dropping it removes redundant write-path index maintenance with no read-path cost.
DROP INDEX idx_transactions_account_id;
