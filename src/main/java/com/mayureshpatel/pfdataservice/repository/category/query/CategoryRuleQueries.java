package com.mayureshpatel.pfdataservice.repository.category.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CategoryRuleQueries {

    // language=SQL
    // PF-313: order is priority desc (higher priority wins), then a specificity tie-breaker desc
    // (more specific match first), then id asc as a final tie-breaker -- without it, two rules
    // sharing both the same priority and the same specificity would have no deterministic order.
    // PF-315: specificity was originally length(cr.keyword) -- a rule now has a *set* of keywords
    // (category_rule_keywords), so the correlated subquery sums their lengths instead. A rule with
    // one keyword behaves identically to before; a multi-keyword rule is treated as more specific
    // in proportion to its combined keyword length, which is a reasonable proxy even though it's a
    // judgment call (no ticket-specified tie-break rule for the multi-keyword case existed).
    public static final String FIND_ALL_BY_USER_ID = """
                select cr.*,
                       c.name as category_name,
                       c.color as category_color,
                       c.icon as category_icon,
                       c.type as category_type
                from category_rules cr
                    left join categories c on cr.category_id = c.id
                where cr.user_id = :userId
                order by cr.priority desc,
                         (select coalesce(sum(length(crk.keyword)), 0)
                          from category_rule_keywords crk
                          where crk.rule_id = cr.id) desc,
                         cr.id asc
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
    // PF-315: a rule's keywords live in category_rule_keywords, not this table -- fetched
    // separately (one query for the whole user, grouped by rule_id in the repository) rather than
    // joined here, since a join would turn this into a one-row-per-keyword result set instead of
    // one-row-per-rule, which the existing RowMapper<CategoryRule> isn't shaped for.
    public static final String FIND_KEYWORDS_BY_USER_ID = """
            select crk.rule_id, crk.keyword
            from category_rule_keywords crk
                join category_rules cr on crk.rule_id = cr.id
            where cr.user_id = :userId
            order by crk.rule_id, crk.id
            """;

    // language=SQL
    public static final String FIND_KEYWORDS_BY_RULE_ID = """
            select keyword
            from category_rule_keywords
            where rule_id = :ruleId
            order by id
            """;

    // language=SQL
    // PF-314: id is intentionally omitted from the column list (was previously bound explicitly,
    // which -- since the caller never actually supplied one -- meant every insert bound literal
    // SQL NULL into a BIGSERIAL column and violated its not-null constraint on every call. Explicit
    // NULL bypasses a column's DEFAULT; only omitting the column entirely lets it apply).
    public static final String INSERT = """
                insert into category_rules (category_id, priority, min_amount, max_amount, match_type, user_id, created_at, updated_at)
                values (:categoryId, :priority, :minAmount, :maxAmount, :matchType, :userId, current_timestamp, current_timestamp)
                returning id
            """;

    // language=SQL
    public static final String INSERT_KEYWORD = """
            insert into category_rule_keywords (rule_id, keyword)
            values (:ruleId, :keyword)
            """;

    // language=SQL
    // PF-315: a rule's keyword set on update is replaced wholesale (delete then re-insert, see
    // DELETE_KEYWORDS_BY_RULE_ID) rather than diffed -- simpler and correct given the small,
    // unordered set size, at the cost of new auto-generated ids for every keyword row on every
    // update. Nothing references a keyword row's own id, so this cost is invisible to callers.
    public static final String DELETE_KEYWORDS_BY_RULE_ID = """
            delete from category_rule_keywords where rule_id = :ruleId
            """;

    // language=SQL
    // PF-314: previously unimplemented (CategoryRuleRepository had no override, so every real call
    // fell through to the interface default's unconditional throw) -- ownership-scoped at the SQL
    // level too, matching the three-layer pattern already established for accounts and merchants.
    public static final String UPDATE = """
            update category_rules
            set category_id = :categoryId,
                priority = :priority,
                min_amount = :minAmount,
                max_amount = :maxAmount,
                match_type = :matchType,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    // category_rule_keywords rows cascade-delete automatically (ON DELETE CASCADE on rule_id) --
    // no separate keyword cleanup needed here.
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
