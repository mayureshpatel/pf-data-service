package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryRuleQueries {

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
                select *
                from category_rules
                where user_id = :userId
                order by priority desc, length(keyword) desc
            """;

    // language=SQL
    public static final String INSERT = """
                insert into category_rules (id, keyword, category_id, priority, user_id, created_at, updated_at)
                values(:id, :keyword, :categoryId, :priority, :userId, current_timestamp, current_timestamp)
                on conflict (id) do update set keyword = excluded.keyword, category_id = excluded.category_id, priority = excluded.priority
            """;

    // language=SQL
    public static final String DELETE = """
            delete from category_rules where id = :id
            """;
}
