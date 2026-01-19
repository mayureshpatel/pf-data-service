-- V14: Robust Seed Data
-- 1. Clean up any existing data for the 'admin' user to ensure a fresh, consistent state.
DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1));
DELETE FROM category_rules WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
DELETE FROM tags WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
DELETE FROM accounts WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
DELETE FROM categories WHERE user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
DELETE FROM users WHERE username = 'admin';

-- 2. Seed Initial User (Username: admin, Password: password)
INSERT INTO users (username, password_hash, email, last_updated_by, last_updated_timestamp)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'admin@example.com', 'system', CURRENT_TIMESTAMP);

-- 3. Seed Categories & Sub-categories for admin
-- Parent Categories
INSERT INTO categories (user_id, name, color) VALUES 
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Housing', '#2196F3'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Food', '#4CAF50'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Transportation', '#F44336'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Entertainment', '#9C27B0'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Income', '#FFD700'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Utilities', '#00BCD4');

-- Sub-categories
INSERT INTO categories (user_id, name, color, parent_id) VALUES 
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Rent', '#42A5F5', (SELECT id FROM categories WHERE name = 'Housing' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Electricity', '#00ACC1', (SELECT id FROM categories WHERE name = 'Utilities' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Water', '#26C6DA', (SELECT id FROM categories WHERE name = 'Utilities' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Groceries', '#66BB6A', (SELECT id FROM categories WHERE name = 'Food' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Dining Out', '#81C784', (SELECT id FROM categories WHERE name = 'Food' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Gas', '#EF5350', (SELECT id FROM categories WHERE name = 'Transportation' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Salary', '#FFF176', (SELECT id FROM categories WHERE name = 'Income' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1)),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Streaming', '#BA68C8', (SELECT id FROM categories WHERE name = 'Entertainment' AND parent_id IS NULL AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1));

-- 4. Seed Accounts for admin
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
VALUES 
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Main Checking', 'CHECKING', 4250.00, 'USD'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'High Yield Savings', 'SAVINGS', 15000.00, 'USD'),
((SELECT id FROM users WHERE username = 'admin' LIMIT 1), 'Travel Card', 'CREDIT_CARD', -120.50, 'USD');

-- 5. Seed 6 Months of Transactions (Aug 2025 - Jan 2026)
-- Salary (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Salary' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    3200.00,
    d::date,
    'Monthly Salary',
    'INCOME'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Rent (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Rent' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    1800.00,
    d::date,
    'Monthly Rent Payment',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Utilities (Variable)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Electricity' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (80 + (random() * 40))::numeric(19,2),
    (d + interval '10 days')::date,
    'Electric Bill',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Groceries (Weekly-ish)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Groceries' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (100 + (random() * 50))::numeric(19,2),
    d::date,
    'Weekly Groceries',
    'EXPENSE'
FROM generate_series('2025-08-03'::date, '2026-01-15'::date, '7 days'::interval) d;

-- Dining Out (Randomized)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Dining Out' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (25 + (random() * 60))::numeric(19,2),
    d::date,
    'Dinner Out',
    'EXPENSE'
FROM generate_series('2025-08-05'::date, '2026-01-15'::date, '5 days'::interval) d;

-- Gas (Bi-weekly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Travel Card' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Gas' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (40 + (random() * 15))::numeric(19,2),
    d::date,
    'Gas Station',
    'EXPENSE'
FROM generate_series('2025-08-02'::date, '2026-01-18'::date, '12 days'::interval) d;

-- Streaming (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Travel Card' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    (SELECT id FROM categories WHERE name = 'Streaming' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1),
    15.99,
    (d + interval '4 days')::date,
    'Netflix Subscription',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- One-time larger expenses
INSERT INTO transactions (account_id, category_id, amount, date, description, type) VALUES
((SELECT id FROM accounts WHERE name = 'Travel Card' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), (SELECT id FROM categories WHERE name = 'Entertainment' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), 450.00, '2025-11-20', 'Concert Tickets', 'EXPENSE'),
((SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), (SELECT id FROM categories WHERE name = 'Housing' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), 320.00, '2025-09-15', 'New Furniture', 'EXPENSE'),
((SELECT id FROM accounts WHERE name = 'Main Checking' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), (SELECT id FROM categories WHERE name = 'Income' AND user_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1) LIMIT 1), 500.00, '2025-12-24', 'Holiday Bonus', 'INCOME');