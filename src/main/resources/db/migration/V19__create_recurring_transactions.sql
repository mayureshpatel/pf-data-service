CREATE TABLE recurring_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_id BIGINT REFERENCES accounts(id),
    merchant_name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    last_date DATE,
    next_date DATE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_recurring_user_next_date ON recurring_transactions(user_id, next_date);
