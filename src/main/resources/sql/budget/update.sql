UPDATE budgets
SET amount = :amount,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id AND deleted_at IS NULL