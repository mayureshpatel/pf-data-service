-- 6. Add Tagging Support (Many-to-Many)
CREATE TABLE tags
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(20),                                -- Optional: UI color for the tag pill
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_tags_name_user UNIQUE (user_id, name) -- Prevent duplicate tag names for same user
);

CREATE TABLE transaction_tags
(
    transaction_id BIGINT NOT NULL,
    tag_id         BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_tt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_tt_tag_id ON transaction_tags (tag_id);