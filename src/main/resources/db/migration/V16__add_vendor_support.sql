-- Add vendor_name to transactions
ALTER TABLE transactions ADD COLUMN vendor_name VARCHAR(100);

-- Create vendor_rules table
CREATE TABLE vendor_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    keyword VARCHAR(255) NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    priority INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_vendor_rules_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_rules_user ON vendor_rules(user_id);
