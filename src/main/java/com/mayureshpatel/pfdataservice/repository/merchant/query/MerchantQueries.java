package com.mayureshpatel.pfdataservice.repository.merchant.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MerchantQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from merchants
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from merchants
            where user_id = :userId
            """;

    // language=SQL
    public static final String COUNT_BY_USER_ID = """
            select count(*)
            from merchants
            where user_id = :userId
            """;

    // language=SQL
    public static final String COUNT_BY_USER_ID_AND_SEARCH = """
            select count(*)
            from merchants
            where user_id = :userId
              and (clean_name ilike :search or original_name ilike :search)
            """;

    // language=SQL
    public static final String FIND_PAGE_BY_USER_ID = """
            select *
            from merchants
            where user_id = :userId
            """;

    // language=SQL
    public static final String FIND_PAGE_BY_USER_ID_AND_SEARCH = """
            select *
            from merchants
            where user_id = :userId
              and (clean_name ilike :search or original_name ilike :search)
            """;

    // language=SQL -- PF-842: distinct clean names for the two-level picker/grouped view. Excludes
    // "" (unreviewed rows have no clean name yet -- not a real group to pick or display).
    public static final String COUNT_DISTINCT_CLEAN_NAMES_BY_USER_ID = """
            select count(distinct clean_name)
            from merchants
            where user_id = :userId
              and clean_name <> ''
            """;

    // language=SQL
    public static final String COUNT_DISTINCT_CLEAN_NAMES_BY_USER_ID_AND_SEARCH = """
            select count(distinct clean_name)
            from merchants
            where user_id = :userId
              and clean_name <> ''
              and clean_name ilike :search
            """;

    // language=SQL
    public static final String FIND_DISTINCT_CLEAN_NAMES_BY_USER_ID = """
            select distinct clean_name
            from merchants
            where user_id = :userId
              and clean_name <> ''
            order by clean_name
            """;

    // language=SQL
    public static final String FIND_DISTINCT_CLEAN_NAMES_BY_USER_ID_AND_SEARCH = """
            select distinct clean_name
            from merchants
            where user_id = :userId
              and clean_name <> ''
              and clean_name ilike :search
            order by clean_name
            """;

    // language=SQL
    public static final String FIND_BY_ID_AND_USER_ID = """
            select *
            from merchants
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAME = """
            select *
            from merchants
            where clean_name = :cleanName
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAME_AND_USER_ID = """
            select *
            from merchants
            where clean_name = :cleanName
              and user_id = :userId
            order by id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAMES_AND_USER_ID = """
            select *
            from merchants
            where clean_name in (:cleanNames)
              and user_id = :userId
            """;

    // language=SQL
    // light-normalized (case-fold + whitespace-collapse) match against original_name -- must
    // stay in exact lockstep with MerchantService.lightNormalize(), which performs the
    // equivalent transformation in application code. note the escaping: inside a text block,
    // '\s' is JEP 378's own literal-space escape, not a regex passthrough -- '\\s+' below is
    // required to actually send the two-character regex '\s+' to postgres.
    public static final String FIND_ALL_BY_NORMALIZED_ORIGINAL_NAME_AND_USER_ID = """
            select *
            from merchants
            where user_id = :userId
              and lower(trim(regexp_replace(original_name, '\\s+', ' ', 'g'))) = :normalizedOriginalName
            order by id
            """;

    // language=SQL
    // batch form of FIND_ALL_BY_NORMALIZED_ORIGINAL_NAME_AND_USER_ID -- same escaping note applies.
    public static final String FIND_ALL_BY_NORMALIZED_ORIGINAL_NAMES_AND_USER_ID = """
            select *
            from merchants
            where user_id = :userId
              and lower(trim(regexp_replace(original_name, '\\s+', ' ', 'g'))) in (:normalizedOriginalNames)
            """;

    // language=SQL -- PF-841: groups by display name (clean name if set, else the merchant's own
    // original name), not merchant id, so merchants deliberately linked under one clean name
    // aggregate into a single row instead of rendering as separate, identically-labeled entries
    // with split totals. Grouping by clean_name alone would be wrong while rows are unreviewed
    // (blank clean_name, the normal state for a freshly-imported merchant per PF-840) -- that
    // would merge every unrelated blank-clean-name row into one meaningless "" bucket. Falling
    // back to original_name (unique per user, idx_merchants_user_original_name) avoids that.
    // min(m.id) is kept only as a stable representative id for the frontend to key/track by --
    // once a group spans multiple merchant rows there's no single "the" id anymore.
    public static final String FIND_MERCHANT_TOTALS = """
            select min(m.id) as representative_merchant_id,
                   coalesce(nullif(m.clean_name, ''), m.original_name) as display_name,
                   sum(t.amount) as total
            from transactions t
            join accounts a on t.account_id = a.id
            join merchants m on t.merchant_id = m.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.date < :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by coalesce(nullif(m.clean_name, ''), m.original_name)
            """;

    // language=SQL -- PF-823: Reports' Merchants tab, aggregated over the full requested range
    // (no row cap). array_remove(..., null) drops the NULL entry array_agg would otherwise
    // contribute for this merchant's uncategorized transactions, so `categories` never contains
    // a null placeholder. PF-841: grouped by display name, not merchant id -- see
    // FIND_MERCHANT_TOTALS's comment above for why (same rationale, same fallback).
    public static final String FIND_MERCHANT_REPORT_DATA = """
            select min(m.id) as representative_merchant_id,
                   coalesce(nullif(m.clean_name, ''), m.original_name) as display_name,
                   sum(t.amount) as total,
                   count(*) as txn_count,
                   array_remove(array_agg(distinct c.name), null) as category_names
            from transactions t
            join accounts a on t.account_id = a.id
            join merchants m on t.merchant_id = m.id
            left join categories c on t.category_id = c.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.date < :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by coalesce(nullif(m.clean_name, ''), m.original_name)
            """;

    // language=SQL
    public static final String INSERT = """
            insert into merchants (user_id, original_name, clean_name)
            values (:userId, :originalName, :name)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update merchants
            set clean_name = :name,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    public static final String DELETE = """
            delete from merchants
            where id = :id
              and user_id = :userId
            """;
}
