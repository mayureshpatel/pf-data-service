-- V14: Robust Seed Data (REVERTED TO PRE-MODIFICATION STATE FOR CHECKSUM)
-- 1. Seed Initial User (Username: admin, Password: password)
INSERT INTO users (username, password_hash, email, last_updated_by, last_updated_timestamp)
SELECT 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'admin@example.com', 'system', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 2. Seed Categories & Sub-categories for admin
-- Parent Categories
INSERT INTO categories (user_id, name, color)
SELECT (SELECT id FROM users WHERE username = 'admin'), name, color
FROM (VALUES 
    ('Housing', '#2196F3'),
    ('Food', '#4CAF50'),
    ('Transportation', '#F44336'),
    ('Entertainment', '#9C27B0'),
    ('Income', '#FFD700'),
    ('Utilities', '#00BCD4')
) AS t(name, color)
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = t.name AND user_id = (SELECT id FROM users WHERE username = 'admin'));

-- Sub-categories
INSERT INTO categories (user_id, name, color, parent_id)
SELECT (SELECT id FROM users WHERE username = 'admin'), name, color, (SELECT id FROM categories WHERE name = p_name AND user_id = (SELECT id FROM users WHERE username = 'admin') AND parent_id IS NULL)
FROM (VALUES 
    ('Rent', '#42A5F5', 'Housing'),
    ('Electricity', '#00ACC1', 'Utilities'),
    ('Water', '#26C6DA', 'Utilities'),
    ('Groceries', '#66BB6A', 'Food'),
    ('Dining Out', '#81C784', 'Food'),
    ('Gas', '#EF5350', 'Transportation'),
    ('Salary', '#FFF176', 'Income'),
    ('Streaming', '#BA68C8', 'Entertainment')
) AS t(name, color, p_name)
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = t.name AND user_id = (SELECT id FROM users WHERE username = 'admin') AND parent_id IS NOT NULL);

-- 3. Seed Accounts for admin
INSERT INTO accounts (user_id, name, type, current_balance, currency_code)
SELECT (SELECT id FROM users WHERE username = 'admin'), name, type, bal, 'USD'
FROM (VALUES 
    ('Main Checking', 'CHECKING', 4250.00),
    ('High Yield Savings', 'SAVINGS', 15000.00),
    ('Travel Card', 'CREDIT_CARD', -120.50)
) AS t(name, type, bal)
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE name = t.name AND user_id = (SELECT id FROM users WHERE username = 'admin'));

-- 4. Seed 6 Months of Transactions (Aug 2025 - Jan 2026)
-- We'll delete existing transactions for these accounts to ensure a clean slate if V13 left crumbs
DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = (SELECT id FROM users WHERE username = 'admin'));

-- Salary (Semi-monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking'),
    (SELECT id FROM categories WHERE name = 'Salary'),
    3200.00,
    d::date,
    'Monthly Salary',
    'INCOME'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Rent (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking'),
    (SELECT id FROM categories WHERE name = 'Rent'),
    1800.00,
    d::date,
    'Monthly Rent Payment',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Utilities (Variable)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking'),
    (SELECT id FROM categories WHERE name = 'Electricity'),
    (80 + (random() * 40))::numeric(19,2),
    (d + interval '10 days')::date,
    'Electric Bill',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- Groceries (Weekly-ish)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking'),
    (SELECT id FROM categories WHERE name = 'Groceries'),
    (100 + (random() * 50))::numeric(19,2),
    d::date,
    'Weekly Groceries',
    'EXPENSE'
FROM generate_series('2025-08-03'::date, '2026-01-15'::date, '7 days'::interval) d;

-- Dining Out (Randomized)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Main Checking'),
    (SELECT id FROM categories WHERE name = 'Dining Out'),
    (25 + (random() * 60))::numeric(19,2),
    d::date,
    'Dinner Out',
    'EXPENSE'
FROM generate_series('2025-08-05'::date, '2026-01-15'::date, '5 days'::interval) d;

-- Gas (Bi-weekly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Travel Card'),
    (SELECT id FROM categories WHERE name = 'Gas'),
    (40 + (random() * 15))::numeric(19,2),
    d::date,
    'Gas Station',
    'EXPENSE'
FROM generate_series('2025-08-02'::date, '2026-01-18'::date, '12 days'::interval) d;

-- Streaming (Monthly)
INSERT INTO transactions (account_id, category_id, amount, date, description, type)
SELECT 
    (SELECT id FROM accounts WHERE name = 'Travel Card'),
    (SELECT id FROM categories WHERE name = 'Streaming'),
    15.99,
    (d + interval '4 days')::date,
    'Netflix Subscription',
    'EXPENSE'
FROM generate_series('2025-08-01'::date, '2026-01-01'::date, '1 month'::interval) d;

-- One-time larger expenses
INSERT INTO transactions (account_id, category_id, amount, date, description, type) VALUES
((SELECT id FROM accounts WHERE name = 'Travel Card'), (SELECT id FROM categories WHERE name = 'Entertainment'), 450.00, '2025-11-20', 'Concert Tickets', 'EXPENSE'),
((SELECT id FROM accounts WHERE name = 'Main Checking'), (SELECT id FROM categories WHERE name = 'Housing'), 320.00, '2025-09-15', 'New Furniture', 'EXPENSE'),
((SELECT id FROM accounts WHERE name = 'Main Checking'), (SELECT id FROM categories WHERE name = 'Income'), 500.00, '2025-12-24', 'Holiday Bonus', 'INCOME');