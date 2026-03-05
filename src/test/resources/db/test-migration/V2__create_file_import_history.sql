CREATE TABLE file_import_history
(
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT       NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    file_hash         VARCHAR(64)  NOT NULL,
    transaction_count INT          NOT NULL,
    imported_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_import_account_hash ON file_import_history (account_id, file_hash);