CREATE TABLE category_rules (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    priority INT DEFAULT 0
);

-- Seed initial rules
INSERT INTO category_rules (keyword, category_name, priority) VALUES
('PUBLIX', 'Groceries', 1),
('KROGER', 'Groceries', 1),
('WHOLE FOODS', 'Groceries', 1),
('TRADER JOE', 'Groceries', 1),
('WALMART', 'Groceries', 1),
('WEGMANS', 'Groceries', 1),
('MCDONALD', 'Dining Out', 1),
('STARBUCKS', 'Dining Out', 1),
('DUNKIN', 'Dining Out', 1),
('TACO BELL', 'Dining Out', 1),
('CHIPOTLE', 'Dining Out', 1),
('UBER EATS', 'Dining Out', 5),
('DOMINO', 'Dining Out', 1),
('AT&T', 'Utilities', 1),
('VERIZON', 'Utilities', 1),
('XFINITY', 'Utilities', 1),
('POWER', 'Utilities', 1),
('WATER', 'Utilities', 1),
('NETFLIX', 'Entertainment', 1),
('SPOTIFY', 'Entertainment', 1),
('STEAM', 'Entertainment', 1),
('NINTENDO', 'Entertainment', 1),
('UBER', 'Transportation', 1),
('LYFT', 'Transportation', 1),
('SHELL', 'Gas', 1),
('CHEVRON', 'Gas', 1),
('EXXON', 'Gas', 1);
