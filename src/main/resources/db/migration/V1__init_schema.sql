-- 1. Create Users Table
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Categories Table
CREATE TABLE categories
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT      NOT NULL,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(20),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. Create Accounts Table
CREATE TABLE accounts
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    type            VARCHAR(20)    NOT NULL, -- CHECKING, SAVINGS, etc.
    current_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 4. Create Transactions Table
CREATE TABLE transactions
(
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT         NOT NULL,
    category_id BIGINT,                  -- Nullable, because a transaction might be uncategorized initially
    amount      NUMERIC(19, 2) NOT NULL,
    date        DATE           NOT NULL,
    description VARCHAR(255),
    type        VARCHAR(20)    NOT NULL, -- CREDIT/DEBIT
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

-- 5. Create Indexes for Performance
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_date ON transactions (date);