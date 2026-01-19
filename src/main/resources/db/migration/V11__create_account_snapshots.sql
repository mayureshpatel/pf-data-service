CREATE TABLE account_snapshots (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_snapshots_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT uq_account_snapshot_date UNIQUE (account_id, snapshot_date)
);

CREATE INDEX idx_snapshots_account_date ON account_snapshots (account_id, snapshot_date);
