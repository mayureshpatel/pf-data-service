package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select
                categories.id,
                categories.user_id,
                categories.parent_id,
                categories.name,
                categories.color,
                categories.icon,
                categories.type,
                parent_category.id as category_parent_id,
                parent_category.name as category_parent_name,
                parent_category.color as category_parent_color,
                parent_category.icon as category_parent_icon,
                parent_category.type as category_parent_type,
                categories.created_at,
                categories.updated_at
            from categories
                left join categories parent_category on parent_category.id = categories.parent_id
            where categories.id = :id
                and categories.user_id = :userId
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select
                categories.id,
                categories.user_id,
                categories.parent_id,
                categories.name,
                categories.color,
                categories.icon,
                categories.type,
                parent_category.id as category_parent_id,
                parent_category.name as category_parent_name,
                parent_category.color as category_parent_color,
                parent_category.icon as category_parent_icon,
                parent_category.type as category_parent_type,
                categories.created_at,
                categories.updated_at
            from categories
                left join categories parent_category on parent_category.id = categories.parent_id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select
                categories.id,
                categories.user_id,
                categories.parent_id,
                categories.name,
                categories.color,
                categories.icon,
                categories.type,
                parent_category.id as category_parent_id,
                parent_category.name as category_parent_name,
                parent_category.color as category_parent_color,
                parent_category.icon as category_parent_icon,
                parent_category.type as category_parent_type,
                categories.created_at,
                categories.updated_at
            from categories
                left join categories parent_category on parent_category.id = categories.parent_id
            where categories.user_id = :userId
            """;

    // language=SQl
    public static final String FIND_ALL_SUB_CATEGORIES = """
            select
                categories.id,
                categories.user_id,
                categories.parent_id,
                categories.name,
                categories.color,
                categories.icon,
                categories.type,
                categories.created_at,
                categories.updated_at
            from categories
            where categories.parent_id is not null
                and categories.user_id = :userId
            """;

    // language=SQL
    public static final String FIND_ALL_PARENT_CATEGORIES = """
            select
                categories.id,
                categories.user_id,
                categories.parent_id,
                categories.name,
                categories.color,
                categories.icon,
                categories.type,
                parent_category.id as category_parent_id,
                parent_category.name as category_parent_name,
                parent_category.color as category_parent_color,
                parent_category.icon as category_parent_icon,
                parent_category.type as category_parent_type,
                categories.created_at,
                categories.updated_at
            from categories
                     left join categories parent_category on parent_category.id = categories.parent_id
            WHERE categories.user_id = :userId
                and parent_category.id is null
            ORDER BY parent_category.id nulls first
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from categories
            where id = :id
                and user_id = :userId
            """;

    // language=SQL
    public static final String INSERT = """
            insert into categories (
                                    name, color, icon, type, user_id, parent_id,
                                    created_at, updated_at)
            values (
                    :name, :color, :icon, :type, :userId, :parentId,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update categories
            set name = :name,
                color = :color,
                icon = :icon,
                type = :type,
                parent_id = :parentId,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
                and user_id = :userId
            """;

    // language=SQL
    public static final String DELETE = """
            delete from categories
            where id = :id
                and user_id = :userId
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*)
            from categories
            where user_id = :userId
            """;
}
