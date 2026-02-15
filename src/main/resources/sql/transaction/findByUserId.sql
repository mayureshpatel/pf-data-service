SELECT t.* FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.user_id = :userId AND t.deleted_at IS NULL
ORDER BY t.date DESC