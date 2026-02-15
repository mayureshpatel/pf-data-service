SELECT
    EXTRACT(YEAR FROM t.date) as year,
    EXTRACT(MONTH FROM t.date) as month,
    SUM(t.amount) as total
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.user_id = :userId
  AND t.date >= :startDate
  AND t.type = 'EXPENSE'
  AND t.deleted_at IS NULL
GROUP BY year, month
ORDER BY year, month