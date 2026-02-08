SELECT code, name, symbol, is_active, created_at
FROM currencies
WHERE code = :code
