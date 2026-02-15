UPDATE transactions
SET amount = :amount,
    date = :date,
    post_date = :postDate,
    description = :description,
    original_vendor_name = :originalVendorName,
    vendor_name = :vendorName,
    type = :type,
    account_id = :accountId,
    category_id = :categoryId,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id AND deleted_at IS NULL