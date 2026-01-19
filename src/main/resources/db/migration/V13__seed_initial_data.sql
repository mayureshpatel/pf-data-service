-- Seed Initial User (Username: admin, Password: password)
INSERT INTO users (username, password_hash, email, last_updated_by, last_updated_timestamp)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'admin@example.com', 'system', CURRENT_TIMESTAMP);

-- Seed Categories for admin
INSERT INTO categories (user_id, name, color)
SELECT id, 'Groceries', '#4CAF50' FROM users WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Dining Out', '#FF9800' FROM users WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Utilities', '#2196F3' FROM users WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Entertainment', '#9C27B0' FROM users WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Transportation', '#F44336' FROM users WHERE username = 'admin';
INSERT INTO categories (user_id, name, color)
SELECT id, 'Gas', '#795548' FROM users WHERE username = 'admin';

-- Seed Accounts for admin
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'Main Checking', 'CHECKING', 5000.00, 'USD' FROM users WHERE username = 'admin';
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'High Yield Savings', 'SAVINGS', 15000.00, 'USD' FROM users WHERE username = 'admin';
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT id, 'Travel Card', 'CREDIT_CARD', -250.00, 'USD' FROM users WHERE username = 'admin';
