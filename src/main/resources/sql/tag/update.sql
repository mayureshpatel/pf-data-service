UPDATE tags
SET name = :name,
    color = :color,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id