-- Add user_id to category_rules to support personalized rules
-- If user_id is NULL, the rule is considered "Global" (system default)

ALTER TABLE category_rules
    ADD COLUMN user_id BIGINT;

ALTER TABLE category_rules
    ADD CONSTRAINT fk_rules_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_rules_user ON category_rules (user_id);
