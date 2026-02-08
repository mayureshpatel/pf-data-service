UPDATE users
SET username = :username,
    email = :email,
    password_hash = :passwordHash,
    last_updated_by = :lastUpdatedBy,
    last_updated_timestamp = CURRENT_TIMESTAMP
WHERE id = :id
    AND deleted_at IS NULL