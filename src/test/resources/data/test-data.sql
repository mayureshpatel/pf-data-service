-- ==========================================
-- 1. USERS
-- ==========================================
-- User 1: The main user we are testing
INSERT INTO users (id, username, password_hash, email, last_updated_by, last_updated_timestamp)
VALUES (1, 'main_user', 'hash123', 'main@test.com', 'system', NOW());

-- User 2: The "Noise" user to ensure data doesn't leak between users
INSERT INTO users (id, username, password_hash, email, last_updated_by, last_updated_timestamp)
VALUES (2, 'noise_user', 'hash456', 'noise@test.com', 'system', NOW());


-- ==========================================
-- 2. CATEGORIES
-- ==========================================
-- User 1 Categories
INSERT INTO categories (id, user_id, name, color) VALUES (100, 1, 'Groceries', '#00FF00');
INSERT INTO categories (id, user_id, name, color) VALUES (101, 1, 'Dining Out', '#FF0000');

-- User 2 Categories (Same name, different ID/User)
INSERT INTO categories (id, user_id, name, color) VALUES (200, 2, 'Groceries', '#0000FF');


-- ==========================================
-- 3. ACCOUNTS
-- ==========================================
-- User 1 Accounts
INSERT INTO accounts (id, user_id, name, type, current_balance)
VALUES (10, 1, 'Primary Checking', 'CHECKING', 1500.00);

-- User 2 Accounts
INSERT INTO accounts (id, user_id, name, type, current_balance)
VALUES (20, 2, 'Savings', 'SAVINGS', 5000.00);


-- ==========================================
-- 4. TRANSACTIONS
-- ==========================================

-- SCENARIO A: Happy Path (User 1, Within Date Range, Expense)
-- Total Groceries: 50.00 + 25.50 = 75.50
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (1001, 10, 100, 50.00, CURRENT_DATE, 'Weekly Groceries', 'EXPENSE');

INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (1002, 10, 100, 25.50, CURRENT_DATE - 1, 'Quick Snack', 'EXPENSE');

-- Total Dining Out: 60.00
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (1003, 10, 101, 60.00, CURRENT_DATE, 'Dinner Date', 'EXPENSE');


-- SCENARIO B: Filter by TYPE (Income should be ignored)
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (1004, 10, NULL, 2000.00, CURRENT_DATE, 'Paycheck', 'INCOME');


-- SCENARIO C: Filter by DATE (Old data should be ignored)
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (1005, 10, 100, 999.00, CURRENT_DATE - 365, 'Last Years Groceries', 'EXPENSE');


-- SCENARIO D: Filter by USER (User 2 data should be ignored)
INSERT INTO transactions (id, account_id, category_id, amount, date, description, type)
VALUES (2001, 20, 200, 1000.00, CURRENT_DATE, 'User 2 Spending', 'EXPENSE');