-- PF-884: remembers which detected recurring patterns a user has dismissed in the review, so a
-- declined pattern stops being suggested. The key is the detected name folded the same way
-- merchant description links fold descriptions (trim, collapse whitespace, lower case), so a
-- dismissal still applies when the bank changes the spacing or case of a description.
CREATE TABLE dismissed_recurring_patterns
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pattern_key VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_dismissed_recurring_patterns_user_key
    ON dismissed_recurring_patterns (user_id, pattern_key);

COMMENT ON TABLE dismissed_recurring_patterns IS
    'Recurring patterns the user dismissed from the detected-patterns review; excluded from future suggestions until restored.';
COMMENT ON COLUMN dismissed_recurring_patterns.pattern_key IS 'Lookup key: trim + casefold + whitespace-collapse of the detected name.';
COMMENT ON COLUMN dismissed_recurring_patterns.name IS 'The detected name as shown to the user, kept so the dismissed list is readable.';
