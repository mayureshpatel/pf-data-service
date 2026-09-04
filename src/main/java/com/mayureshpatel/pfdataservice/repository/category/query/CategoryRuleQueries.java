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
    // PF-314: id is intentionally omitted from the column list (was previously bound explicitly,
    // which -- since the caller never actually supplied one -- meant every insert bound literal
    // SQL NULL into a BIGSERIAL column and violated its not-null constraint on every call. Explicit
    // NULL bypasses a column's DEFAULT; only omitting the column entirely lets it apply).
    public static final String INSERT = """
                insert into category_rules (keyword, category_id, priority, min_amount, max_amount, user_id, created_at, updated_at)
                values (:keyword, :categoryId, :priority, :minAmount, :maxAmount, :userId, current_timestamp, current_timestamp)
                returning id
            """;

    // language=SQL
    // PF-314: previously unimplemented (CategoryRuleRepository had no override, so every real call
    // fell through to the interface default's unconditional throw) -- ownership-scoped at the SQL
    // level too, matching the three-layer pattern already established for accounts and merchants.
    public static final String UPDATE = """
            update category_rules
            set keyword = :keyword,
                category_id = :categoryId,
                priority = :priority,
                min_amount = :minAmount,
                max_amount = :maxAmount,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and user_id = :userId
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
