INSERT INTO currencies (code, name, symbol, is_active)
VALUES (:code, :name, :symbol, :isActive)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    symbol = EXCLUDED.symbol,
    is_active = EXCLUDED.is_active
