-- V32: Refactor category_rules table
-- Changes:
--   - category_name (VARCHAR) replaced by category_id (FK → categories)
--
-- Note: categories are user-scoped (every category row has a user_id).
--       Global rules (category_rules.user_id IS NULL) were seeded with category names
--       like 'Groceries', 'Dining Out', etc. but have no corresponding row in categories,
--       so their category_id will remain NULL after migration.
--       User-specific rules are matched by (user_id, name).

-- ====================================================================================
-- 1. ADD category_id COLUMN (nullable — global rules cannot resolve to a category row)
-- ====================================================================================
ALTER TABLE category_rules ADD COLUMN category_id BIGINT;

-- ====================================================================================
-- 2. BEST-EFFORT MATCH FOR USER-SPECIFIC RULES
--    Matches on (user_id, category_name = categories.name)
-- ====================================================================================
UPDATE category_rules cr
SET category_id = c.id
FROM categories c
WHERE cr.user_id IS NOT NULL
  AND cr.user_id = c.user_id
  AND cr.category_name = c.name;

-- ====================================================================================
-- 3. ADD FOREIGN KEY CONSTRAINT
-- ====================================================================================
ALTER TABLE category_rules
    ADD CONSTRAINT fk_category_rules_category
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX idx_category_rules_category_id ON category_rules(category_id);

-- ====================================================================================
-- 4. DROP category_name (replaced by category_id)
-- ====================================================================================
ALTER TABLE category_rules DROP COLUMN category_name;
