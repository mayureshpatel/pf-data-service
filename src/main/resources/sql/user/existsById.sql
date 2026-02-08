SELECT COUNT(*)
FROM users
WHERE id = :id
    AND deleted_at IS NULL