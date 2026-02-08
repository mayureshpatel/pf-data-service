SELECT code, name, symbol, is_active, created_at
FROM currencies
WHERE is_active = :isActive
ORDER BY code
