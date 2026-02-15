INSERT INTO categories (name, color, icon, type, user_id, parent_id, created_at, updated_at)
VALUES (:name, :color, :icon, :type, :userId, :parentId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)