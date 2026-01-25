ALTER TABLE categories ADD COLUMN icon VARCHAR(50);
ALTER TABLE categories ADD COLUMN type VARCHAR(20) DEFAULT 'EXPENSE';

-- Update existing categories to have a default type
UPDATE categories SET type = 'EXPENSE';
