package com.mayureshpatel.pfdataservice.repository.merchant.query;

import lombok.NoArgsConstructor;

/**
 * SQL query constants for {@code MerchantRuleRepository}. A rule's keyword set lives in its own
 * {@code merchant_rule_keywords} table, fetched and replaced wholesale via the separate
 * {@code *_KEYWORD*} queries rather than joined into the rule's row, mirroring
 * {@code CategoryRuleQueries}.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MerchantRuleQueries {

    // language=SQL
    // order is the same as category rules (and RuleOrdering): priority desc, summed keyword length
    // desc as the specificity tie-break, then id asc so a full tie stays deterministic.
    public static final String FIND_ALL_BY_USER_ID = """
                select mr.*,
                       m.name as merchant_name,
                       m.city as merchant_city,
                       m.state as merchant_state,
                       m.postal_code as merchant_postal_code,
                       m.country as merchant_country
                from merchant_rules mr
                    left join merchants m on mr.merchant_id = m.id
                where mr.user_id = :userId
                order by mr.priority desc,
                         (select coalesce(sum(length(mrk.keyword)), 0)
                          from merchant_rule_keywords mrk
                          where mrk.rule_id = mr.id) desc,
                         mr.id asc
            """;

    // language=SQL
    public static final String FIND_BY_ID = """
                select mr.*,
                       m.name as merchant_name,
                       m.city as merchant_city,
                       m.state as merchant_state,
                       m.postal_code as merchant_postal_code,
                       m.country as merchant_country
                from merchant_rules mr
                    left join merchants m on mr.merchant_id = m.id
                where mr.id = :id
            """;

    // language=SQL
    // fetched separately and grouped by rule_id in the repository, since a join would turn the
    // one-row-per-rule result into one-row-per-keyword.
    public static final String FIND_KEYWORDS_BY_USER_ID = """
            select mrk.rule_id, mrk.keyword
            from merchant_rule_keywords mrk
                join merchant_rules mr on mrk.rule_id = mr.id
            where mr.user_id = :userId
            order by mrk.rule_id, mrk.id
            """;

    // language=SQL
    public static final String FIND_KEYWORDS_BY_RULE_ID = """
            select keyword
            from merchant_rule_keywords
            where rule_id = :ruleId
            order by id
            """;

    // language=SQL
    // id is omitted from the column list so the BIGSERIAL default applies
    public static final String INSERT = """
                insert into merchant_rules (merchant_id, priority, min_amount, max_amount, match_type, user_id, created_at, updated_at)
                values (:merchantId, :priority, :minAmount, :maxAmount, :matchType, :userId, current_timestamp, current_timestamp)
                returning id
            """;

    // language=SQL
    public static final String INSERT_KEYWORD = """
            insert into merchant_rule_keywords (rule_id, keyword)
            values (:ruleId, :keyword)
            """;

    // language=SQL
    // the keyword set is replaced wholesale on update (delete then re-insert) rather than diffed
    public static final String DELETE_KEYWORDS_BY_RULE_ID = """
            delete from merchant_rule_keywords where rule_id = :ruleId
            """;

    // language=SQL
    // ownership-scoped at the sql level too
    public static final String UPDATE = """
            update merchant_rules
            set merchant_id = :merchantId,
                priority = :priority,
                min_amount = :minAmount,
                max_amount = :maxAmount,
                match_type = :matchType,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    // merchant_rule_keywords rows cascade-delete with the rule (ON DELETE CASCADE on rule_id)
    public static final String DELETE = """
            delete from merchant_rules where id = :id and user_id = :userId
            """;
}
