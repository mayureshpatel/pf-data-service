-- V42: Deliberate merchant identity, phase 1 contract close-out (PF-845, PF-EPIC-048)
-- Purpose: drops the old original_name/clean_name identity now that PF-845's repository/service
--          layer no longer references either column. Safe only because this ships together with
--          that rewrite in the same PR -- see PF-844's own Update note for why V42 was deferred
--          out of that earlier ticket in the first place.
--
-- Pre-flight check (run by hand first, not part of this migration):
--   SELECT count(*) FROM merchants WHERE user_id IS NULL;
--   -- must be 0 before this migration runs against a real database. If it isn't, decide
--   -- explicitly what happens to those rows (reassign to an owner, or delete if truly unused) --
--   -- don't let the NOT NULL constraint below fail silently into a stuck deploy.

ALTER TABLE merchants ALTER COLUMN name SET NOT NULL;
ALTER TABLE merchants ALTER COLUMN user_id SET NOT NULL;

DROP INDEX IF EXISTS idx_merchants_global_original_name;
DROP INDEX IF EXISTS idx_merchants_user_original_name;
DROP INDEX IF EXISTS idx_merchants_user_id_clean_name;

ALTER TABLE merchants DROP COLUMN original_name;
ALTER TABLE merchants DROP COLUMN clean_name;

CREATE INDEX idx_merchants_user_id_name ON merchants (user_id, name);
