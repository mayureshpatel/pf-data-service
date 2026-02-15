package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from categories
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from categories
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from categories
            where user_id = :userId
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from categories
            where id = :id
            """;

    // language=SQL
    public static final String INSERT = """
            insert into categories (
                                    name, color, icon, type, user_id, parent_id,
                                    created_at, updated_at)
            values (
                    :name, :color, :icon, :type, :userId, :parentId,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
            """;

    // language=SQL
    public static final String DELETE = """
            delete from categories
            where id = :id
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*)
            from categories
            """;
}
