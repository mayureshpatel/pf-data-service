package com.mayureshpatel.pfdataservice.repository.tag.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TagQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from tags
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from tags
            where user_id = :userId
            order by name
            """;

    // language=SQL
    public static final String INSERT = """
            insert into tags (user_id, name, color, created_at, updated_at)
            values (:userId, :name, :color, current_timestamp, current_timestamp)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update tags
            set name = :name,
                color = :color,
                updated_at = current_timestamp
            where id = :id
            """;

    // language=SQL
    public static final String DELETE = """
            delete from tags
            where id = :id
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*) from tags
            """;
}
