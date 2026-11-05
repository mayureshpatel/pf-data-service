-- V45: Merchant rules (PF-904, PF-EPIC-049)
-- Purpose: user-written rules that point keywords at ONE existing merchant, mirroring category
--          rules (V37/V38): a keyword set per rule combined by an AND/OR match_type, a priority and
--          an optional min/max amount range. Storage only -- matching and apply logic come later.
--          Deleting a merchant (or the owning user) deletes its rules, and a rule's keywords go
--          with the rule.

CREATE TABLE merchant_rules
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant_id BIGINT         NOT NULL REFERENCES merchants (id) ON DELETE CASCADE,
    match_type  VARCHAR(3)     NOT NULL DEFAULT 'OR',
    priority    INT            NOT NULL DEFAULT 0,
    min_amount  NUMERIC(19, 2),
    max_amount  NUMERIC(19, 2),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_rules_user_id ON merchant_rules (user_id);
CREATE INDEX idx_merchant_rules_merchant_id ON merchant_rules (merchant_id);

CREATE TABLE merchant_rule_keywords
(
    id      BIGSERIAL PRIMARY KEY,
    rule_id BIGINT       NOT NULL REFERENCES merchant_rules (id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL
);

CREATE INDEX idx_merchant_rule_keywords_rule_id ON merchant_rule_keywords (rule_id);

COMMENT ON TABLE merchant_rules IS
    'User-written rules that assign one merchant to transactions whose description matches the rule keywords (per match_type) and whose absolute amount is within the optional range.';
COMMENT ON COLUMN merchant_rules.match_type IS 'How the keywords combine: AND requires all, OR requires at least one.';
COMMENT ON COLUMN merchant_rules.priority IS 'Higher wins when several rules match; ties break on summed keyword length, then id.';
