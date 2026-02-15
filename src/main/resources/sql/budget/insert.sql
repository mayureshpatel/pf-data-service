INSERT INTO budgets (user_id, category_id, amount, month, year, created_at, updated_at)
VALUES (:userId, :categoryId, :amount, :month, :year, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)