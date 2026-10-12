-- PF-820: MerchantRepository.findAllByUserId's search ("name ilike :search or city ilike :search",
-- a leading-wildcard %term% match) is the first production code path to run a fuzzy/substring
-- search against merchants, and no index currently supports it -- a B-tree can't serve a
-- leading-wildcard ILIKE, so every search-by-name request does a sequential scan.
--
-- Targets `name`/`city` (PF-845's merged identity columns), not the `clean_name`/`original_name`
-- pair this ticket was originally filed against -- those columns no longer exist as of V42.
--
-- pg_trgm is already enabled (V10__add_reporting_indices.sql); mirrors that migration's own
-- idx_transactions_description_trgm pattern exactly.
CREATE INDEX idx_merchants_name_trgm ON merchants USING GIN (name gin_trgm_ops);
CREATE INDEX idx_merchants_city_trgm ON merchants USING GIN (city gin_trgm_ops);
