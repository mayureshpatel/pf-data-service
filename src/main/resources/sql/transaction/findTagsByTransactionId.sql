SELECT t.*
FROM tags t
JOIN transaction_tags tt ON t.id = tt.tag_id
WHERE tt.transaction_id = :transactionId