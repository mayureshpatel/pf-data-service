INSERT INTO vendor_rules (keyword, vendor_name, priority, user_id, created_at, updated_at)
VALUES (:keyword, :vendorName, :priority, :userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)