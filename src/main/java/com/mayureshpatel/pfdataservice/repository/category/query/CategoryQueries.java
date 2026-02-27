package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from categories
            where id = :id
                and user_id = :userId
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

    // language=SQl
    public static final String FIND_ALL_SUB_CATEGORIES = """
            select *
            from categories
            where parent_id is not null
                and user_id = :userId
            """;

    //language=SQL
    public static final String FIND_ALL_WITH_PARENT = """
          SELECT
              c.id,
              c.name,
              c.type,
              c.user_id,
              c.parent_id,
              c.color,
              c.icon,
              c.created_at,
              c.updated_at,
              p.name  AS parent_name,
              p.type  AS parent_type,
              p.color AS parent_color,
              p.icon  AS parent_icon
          FROM categories c
          LEFT JOIN categories p ON c.parent_id = p.id
          WHERE c.user_id = :userId
          ORDER BY p.id NULLS FIRST, c.name
          """;

    // language=SQL
    public static final String FIND_ALL_PARENT_CATEGORIES = """
            select *
            from categories
            where id in (select parent_id from categories where parent_id is not null)
            and user_id = :userId
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
