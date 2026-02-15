UPDATE accounts
SET name = :name,
    type = :type,
    current_balance = :currentBalance,
    currency_code = :currencyCode,
    bank_name = :bankName,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = :updatedBy
WHERE id = :id AND version = :version AND deleted_at IS NULL