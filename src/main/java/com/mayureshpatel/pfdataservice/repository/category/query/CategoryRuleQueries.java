package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryRuleQueries {

    // language=SQL
    // PF-313: order is priority desc (higher priority wins), then keyword length desc (more
    // specific match first), then id asc as a final tie-breaker -- without it, two rules sharing
    // both the same priority and the same keyword length would have no deterministic order at all.
    public static final String FIND_ALL_BY_USER_ID = """
                select cr.*,
                       c.name as category_name,
                       c.color as category_color,
                       c.icon as category_icon,
                       c.type as category_type
                from category_rules cr
                    left join categories c on cr.category_id = c.id
                where cr.user_id = :userId
                order by cr.priority desc, length(cr.keyword) desc, cr.id asc
            """;

    // language=SQL
    public static final String FIND_BY_ID = """
                select cr.*,
                       c.name as category_name,
                       c.color as category_color,
                       c.icon as category_icon,
                       c.type as category_type
                from category_rules cr
                    left join categories c on cr.category_id = c.id
                where cr.id = :id
            """;

    // language=SQL
    public static final String INSERT = """
                insert into category_rules (id, keyword, category_id, priority, user_id, created_at, updated_at)
                values(:id, :keyword, :categoryId, :priority, :userId, current_timestamp, current_timestamp)
                on conflict (id) do update set keyword = excluded.keyword, category_id = excluded.category_id, priority = excluded.priority
            """;

    // language=SQL
    public static final String DELETE = """
            delete from category_rules where id = :id and user_id = :userId
            """;

    // language=SQL
    public static final String COUNT_BY_CATEGORY_ID = """
            select count(*)
            from category_rules
            where category_id = :categoryId
            """;
}
