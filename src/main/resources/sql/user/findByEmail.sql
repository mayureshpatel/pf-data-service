SELECT *
FROM users
WHERE email = :email
  AND deleted_at IS NULL