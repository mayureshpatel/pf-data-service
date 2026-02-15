SELECT
    COALESCE(c.name, 'Uncategorized') as category_name,
    SUM(t.amount) as total
FROM transactions t
LEFT JOIN categories c ON t.category_id = c.id
JOIN accounts a ON t.account_id = a.id
WHERE a.user_id = :userId
  AND t.date BETWEEN :startDate AND :endDate
  AND t.type = 'EXPENSE'
  AND t.deleted_at IS NULL
GROUP BY c.name
ORDER BY total DESC