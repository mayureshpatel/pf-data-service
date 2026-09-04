-- V36: Document category_rules.priority's direction (PF-313)
-- Purpose: the column's ordering direction (higher priority wins) was never written down anywhere
--          in the schema -- CategoryRuleQueries.FIND_ALL_BY_USER_ID has always sorted by it
--          correctly (verified by full code trace, not a fix), but nothing said so for a reader
--          looking at the schema alone.

COMMENT ON COLUMN category_rules.priority IS
    'Match-order weight when multiple rules match the same transaction description -- higher '
        'priority wins. Ties break by keyword length (longer/more specific first), then by id '
        '(oldest rule first) for full determinism. Defaults to 0.';
