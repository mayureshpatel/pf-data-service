-- V39: Index on (user_id, clean_name) (PF-840)
-- Supports the merchant identity rework's new clean-name-keyed queries -- the distinct-clean-names
-- picker and the grouped Merchants page view (PF-842), both of which now filter/sort by clean_name
-- at real request volume. Purely additive: no data change, no backfill. clean_name has no index of
-- any kind today; only original_name does (idx_merchants_user_original_name, V29).

CREATE INDEX idx_merchants_user_id_clean_name ON merchants (user_id, clean_name);
