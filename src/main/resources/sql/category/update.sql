UPDATE categories
SET name = :name,
    color = :color,
    icon = :icon,
    type = :type,
    parent_id = :parentId,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id