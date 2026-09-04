-- V37: Add amount-range conditions to category_rules (PF-314)
-- Purpose: an optional min/max amount a transaction must fall within (in addition to the existing
--          keyword match) for a rule to apply -- e.g. "Amazon" under $20 -> Household, over $100
--          -> Electronics. Both nullable: a rule with neither set matches on keyword alone, exactly
--          as it did before this migration.

ALTER TABLE category_rules
    ADD COLUMN min_amount NUMERIC(19, 2),
    ADD COLUMN max_amount NUMERIC(19, 2);
