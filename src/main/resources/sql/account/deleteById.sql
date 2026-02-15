UPDATE accounts
SET deleted_at = CURRENT_TIMESTAMP,
    deleted_by = :deletedBy
WHERE id = :id AND deleted_at IS NULL