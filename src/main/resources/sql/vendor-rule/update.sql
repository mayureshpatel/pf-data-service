UPDATE vendor_rules
SET keyword = :keyword,
    vendor_name = :vendorName,
    priority = :priority,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id