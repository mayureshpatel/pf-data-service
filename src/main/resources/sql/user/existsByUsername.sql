SELECT COUNT(*)
FROM users
WHERE username = :username
    AND deleted_at IS NULL