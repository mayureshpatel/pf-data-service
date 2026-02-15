SELECT COALESCE(SUM(t.amount), 0)
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.user_id = :userId
    AND t.date BETWEEN :startDate AND :endDate
    AND t.type = :type
    AND t.deleted_at IS NULL