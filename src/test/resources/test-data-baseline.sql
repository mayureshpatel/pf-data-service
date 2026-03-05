-- PF DATA SERVICE: TEST DATA BASELINE
-- This script provides a rich dataset for meaningful repository layer testing with Testcontainers.

-- 1. CLEANUP
TRUNCATE TABLE transaction_tags, tags, transactions, recurring_transactions, budgets, category_rules, categories, file_import_history, accounts, merchants, account_types, currencies, users RESTART IDENTITY CASCADE;

-- 2. USERS
INSERT INTO users (id, username, password_hash, email, role, last_updated_by, last_updated_timestamp)
VALUES (1, 'testuser', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'test@example.com', 'USER', 'system', CURRENT_TIMESTAMP),
       (2, 'otheruser', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'other@example.com', 'USER', 'system', CURRENT_TIMESTAMP);

-- 3. ACCOUNT TYPES & CURRENCIES
INSERT INTO account_types (code, label, icon, color, is_asset, sort_order)
VALUES ('CHECKING', 'Checking', 'pi-wallet', 'text-blue-600', true, 1),
       ('SAVINGS', 'Savings', 'pi-piggy-bank', 'text-green-600', true, 2),
       ('CREDIT_CARD', 'Credit Card', 'pi-credit-card', 'text-purple-600', false, 3),
       ('INVESTMENT', 'Investment', 'pi-chart-line', 'text-orange-600', true, 4),
       ('CASH', 'Cash', 'pi-money-bill', 'text-gray-600', true, 5);

INSERT INTO currencies (code, name, symbol)
VALUES ('USD', 'US Dollar', '$');

-- 4. CATEGORIES
-- Parents
INSERT INTO categories (id, user_id, name, color, type)
VALUES (1, 1, 'Housing', '#2196F3', 'EXPENSE'),
       (2, 1, 'Food', '#4CAF50', 'EXPENSE'),
       (3, 1, 'Transportation', '#F44336', 'EXPENSE'),
       (4, 1, 'Income', '#FFD700', 'INCOME'),
       (5, 1, 'Transfer', '#9E9E9E', 'TRANSFER');

-- Subs
INSERT INTO categories (id, user_id, name, color, parent_id, type)
VALUES (6, 1, 'Rent', '#42A5F5', 1, 'EXPENSE'),
       (7, 1, 'Groceries', '#66BB6A', 2, 'EXPENSE'),
       (8, 1, 'Dining Out', '#81C784', 2, 'EXPENSE'),
       (9, 1, 'Gas', '#EF5350', 3, 'EXPENSE'),
       (10, 1, 'Salary', '#FFF176', 4, 'INCOME');

-- User 2
INSERT INTO categories (id, user_id, name, color, type)
VALUES (11, 2, 'User2 Private', '#000000', 'EXPENSE');

-- 5. ACCOUNTS
INSERT INTO accounts (id, user_id, name, type, current_balance, currency_code)
VALUES (1, 1, 'Main Checking', 'CHECKING', 5000.00, 'USD'),
       (2, 1, 'Rainy Day Savings', 'SAVINGS', 10000.00, 'USD'),
       (3, 1, 'Credit Card', 'CREDIT_CARD', -500.00, 'USD'),
       (4, 2, 'User2 Account', 'CHECKING', 100.00, 'USD');

-- 6. MERCHANTS
INSERT INTO merchants (id, user_id, original_name, clean_name)
VALUES (1, NULL, 'WHOLEFDS 1234', 'Whole Foods'),
       (2, NULL, 'AMZN MKTP', 'Amazon'),
       (3, NULL, 'SHELL OIL', 'Shell'),
       (4, 1, 'LOCAL CAFE', 'My Favorite Cafe');

-- 7. TRANSACTIONS
-- User 1 Rent (Expense) - Every month from 2025-09 to 2026-02
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 6, NULL, 1500.00, d + interval '1 day', 'Monthly Rent', 'EXPENSE'
FROM generate_series('2025-09-01'::timestamptz, '2026-02-01'::timestamptz, '1 month'::interval) d;

-- User 1 Groceries (Linked to Whole Foods)
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 7, 1, 125.00, d, 'Grocery Run', 'EXPENSE'
FROM generate_series('2025-09-05'::timestamptz, '2026-02-20'::timestamptz, '1 week'::interval) d;

-- User 1 Salary
INSERT INTO transactions (account_id, category_id, merchant_id, amount, date, description, type)
SELECT 1, 10, NULL, 3000.00, d, 'Monthly Paycheck', 'INCOME'
FROM generate_series('2025-09-01'::timestamptz, '2026-02-01'::timestamptz, '1 month'::interval) d;

-- Specific Transactions for precise matching
INSERT INTO transactions (id, account_id, category_id, merchant_id, amount, date, description, type)
VALUES (1000, 1, 8, 4, 25.50, '2026-03-01 10:00:00+00', 'Morning Coffee', 'EXPENSE'),
       (1001, 3, 9, 3, 45.00, '2026-03-02 15:30:00+00', 'Gas Fill-up', 'EXPENSE');

-- 8. CATEGORY RULES
INSERT INTO category_rules (id, user_id, keyword, category_id, priority)
VALUES (1, 1, 'WHOLEFDS', 7, 10),
       (2, 1, 'SHELL', 9, 5),
       (3, 1, 'CAFE', 8, 1);

-- 9. RECURRING TRANSACTIONS
INSERT INTO recurring_transactions (id, user_id, account_id, merchant_id, amount, frequency, next_date, active)
VALUES (1, 1, 1, 2, 15.99, 'MONTHLY', '2026-04-01', true),
       (2, 1, 3, 3, 50.00, 'WEEKLY', '2026-03-10', true);

-- 10. TAGS
INSERT INTO tags (id, user_id, name, color)
VALUES (1, 1, 'Vacation', '#FFEB3B'),
       (2, 1, 'Work', '#9C27B0');

INSERT INTO transaction_tags (transaction_id, tag_id)
VALUES (1001, 1);

-- 11. RESTART IDENTITY SEQUENCES
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('accounts_id_seq', (SELECT MAX(id) FROM accounts));
SELECT setval('merchants_id_seq', (SELECT MAX(id) FROM merchants));
SELECT setval('transactions_id_seq', (SELECT MAX(id) FROM transactions));
SELECT setval('tags_id_seq', (SELECT MAX(id) FROM tags));
SELECT setval('category_rules_id_seq', (SELECT MAX(id) FROM category_rules));
SELECT setval('recurring_transactions_id_seq', (SELECT MAX(id) FROM recurring_transactions));
