-- V38: Multi-keyword AND/OR category rules (PF-315)
-- Replaces the single `keyword` column with a proper one-to-many keyword set per rule, plus an
-- explicit match_type (AND/OR) for how that set combines. Full migration, not additive: a
-- single-keyword rule becomes the degenerate case -- exactly one row in
-- category_rule_keywords, match_type = 'OR' -- which matches its prior, implicit
-- single-keyword-always-matches behavior exactly. No dual representation kept around.

CREATE TABLE category_rule_keywords
(
    id      BIGSERIAL PRIMARY KEY,
    rule_id BIGINT       NOT NULL REFERENCES category_rules (id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL
);

CREATE INDEX idx_category_rule_keywords_rule_id ON category_rule_keywords (rule_id);

ALTER TABLE category_rules
    ADD COLUMN match_type VARCHAR(3) NOT NULL DEFAULT 'OR';

-- backfill: every existing rule's single keyword becomes its one keyword row
INSERT INTO category_rule_keywords (rule_id, keyword)
SELECT id, keyword
FROM category_rules
WHERE keyword IS NOT NULL;

ALTER TABLE category_rules
    DROP COLUMN keyword;
