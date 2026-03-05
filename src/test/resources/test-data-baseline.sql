-- PF DATA SERVICE: TEST DATA BASELINE
-- This script provides a rich dataset for meaningful repository layer testing with Testcontainers.

-- 1. CLEANUP
TRUNCATE TABLE transactions, transaction_tags, tags, category_rules, recurring_transactions, categories, account_snapshots, file_import_history, merchants, accounts, users RESTART IDENTITY CASCADE;

-- 2. USERS
-- Password is 'Password1!' hashed
INSERT INTO users (id, username, password_hash, email, last_updated_by, last_updated_timestamp)
VALUES (1, 'testuser', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'test@example.com', 'system', CURRENT_TIMESTAMP),
       (2, 'otheruser', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'other@example.com', 'system', CURRENT_TIMESTAMP);

-- 3. CATEGORIES
-- User 1 Parent Categories
INSERT INTO categories (id, user_id, name, color, type)
VALUES (1, 1, 'Housing', '#2196F3', 'EXPENSE'),
       (2, 1, 'Food', '#4CAF50', 'EXPENSE'),
       (3, 1, 'Transportation', '#F44336', 'EXPENSE'),
       (4, 1, 'Income', '#FFD700', 'INCOME'),
       (5, 1, 'Transfer', '#9E9E9E', 'TRANSFER');

-- User 1 Sub-categories
INSERT INTO categories (id, user_id, name, color, parent_id, type)
VALUES (6, 1, 'Rent', '#42A5F5', 1, 'EXPENSE'),
       (7, 1, 'Groceries', '#66BB6A', 2, 'EXPENSE'),
       (8, 1, 'Dining Out', '#81C784', 2, 'EXPENSE'),
       (9, 1, 'Gas', '#EF5350', 3, 'EXPENSE'),
       (10, 1, 'Salary', '#FFF176', 4, 'INCOME');

-- User 2 Category (Isolation Test)
INSERT INTO categories (id, user_id, name, color, type)
VALUES (11, 2, 'User2 Private', '#000000', 'EXPENSE');

-- 4. ACCOUNTS
INSERT INTO accounts (id, user_id, name, type, current_balance, currency_code)
VALUES (1, 1, 'Main Checking', 'CHECKING', 5000.00, 'USD'),
       (2, 1, 'Rainy Day Savings', 'SAVINGS', 10000.00, 'USD'),
       (3, 1, 'Credit Card', 'CREDIT_CARD', -500.00, 'USD'),
       (4, 2, 'User2 Account', 'CHECKING', 100.00, 'USD');

-- 5. MERCHANTS
-- Global Merchants
INSERT INTO merchants (id, user_id, original_name, clean_name)
VALUES (1, NULL, 'WHOLEFDS 1234', 'Whole Foods'),
       (2, NULL, 'AMZN MKTP', 'Amazon'),
       (3, NULL, 'SHELL OIL', 'Shell');

-- User 1 Custom Merchants
INSERT INTO merchants (id, user_id, original_name, clean_name)
VALUES (4, 1, 'LOCAL CAFE', 'My Favorite Cafe');

-- 6. TRANSACTIONS (6 Months: 2025-09 to 2026-02)
-- User 1 Salary (Income)
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 10, NULL, 3000.00, d, 'Monthly Paycheck', 'INCOME'
FROM generate_series('2025-09-01'::timestamptz, '2026-02-01'::timestamptz, '1 month'::interval) d;

-- User 1 Rent (Expense)
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 6, NULL, 1500.00, d + interval '1 day', 'Monthly Rent', 'EXPENSE'
FROM generate_series('2025-09-01'::timestamptz, '2026-02-01'::timestamptz, '1 month'::interval) d;

-- User 1 Groceries (Varied)
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 7, 1, 100.00 + (random() * 50), d, 'Grocery Run', 'EXPENSE'
FROM generate_series('2025-09-05'::timestamptz, '2026-02-20'::timestamptz, '1 week'::interval) d;

-- User 1 Specific Transactions for precise matching tests
INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type)
VALUES (1000, 1, 8, 4, 25.50, '2026-03-01 10:00:00+00', 'Morning Coffee', 'EXPENSE'),
       (1001, 3, 9, 3, 45.00, '2026-03-02 15:30:00+00', 'Gas Fill-up', 'EXPENSE'),
       (1002, 1, NULL, NULL, 500.00, '2026-03-03 12:00:00+00', 'ATM Deposit', 'INCOME');

-- Transfers
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (2000, 1, 5, 1000.00, '2026-02-15 09:00:00+00', 'Transfer to Savings', 'TRANSFER_OUT'),
       (2001, 2, 5, 1000.00, '2026-02-15 09:05:00+00', 'Transfer from Checking', 'TRANSFER_IN');

-- 7. CATEGORY RULES
INSERT INTO category_rules (user_id, keyword, category_id, priority)
VALUES (1, 'WHOLEFDS', 7, 10),
       (1, 'SHELL', 9, 5),
       (1, 'CAFE', 8, 1);

-- 8. RECURRING TRANSACTIONS
INSERT INTO recurring_transactions (user_id, account_id, merchant_id, amount, frequency, next_date, active)
VALUES (1, 1, 2, 15.99, 'MONTHLY', '2026-04-01', true),
       (1, 3, 3, 50.00, 'WEEKLY', '2026-03-10', true);

-- 9. TAGS
INSERT INTO tags (id, user_id, name, color)
VALUES (1, 1, 'Vacation', '#FFEB3B'),
       (2, 1, 'Work', '#9C27B0');

INSERT INTO transaction_tags (transaction_id, tag_id)
VALUES (1001, 1);

-- 10. RESTART IDENTITY SEQUENCES
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('accounts_id_seq', (SELECT MAX(id) FROM accounts));
SELECT setval('merchants_id_seq', (SELECT MAX(id) FROM merchants));
SELECT setval('transactions_id_seq', (SELECT MAX(id) FROM transactions));
SELECT setval('tags_id_seq', (SELECT MAX(id) FROM tags));
