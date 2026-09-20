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
              and (name ilike :search or city ilike :search)
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
              and (name ilike :search or city ilike :search)
            """;

    // language=SQL
    public static final String FIND_BY_ID_AND_USER_ID = """
            select *
            from merchants
            where id = :id
              and user_id = :userId
            """;

    // language=SQL -- PF-845: merchants are deliberately user-owned and can no longer fragment, so
    // grouping goes back to the merchant's own id/name directly. Column aliases kept as
    // representative_merchant_id/display_name (not renamed to merchant_id/name) so the wire
    // contract stays stable for the frontend until PF-847 touches it -- see MerchantBreakdownDto's
    // own Javadoc.
    public static final String FIND_MERCHANT_TOTALS = """
            select m.id as representative_merchant_id,
                   m.name as display_name,
                   sum(t.amount) as total
            from transactions t
            join accounts a on t.account_id = a.id
            join merchants m on t.merchant_id = m.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.date < :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by m.id, m.name
            """;

    // language=SQL -- PF-823/PF-845: same simplification as FIND_MERCHANT_TOTALS above.
    public static final String FIND_MERCHANT_REPORT_DATA = """
            select m.id as representative_merchant_id,
                   m.name as display_name,
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
            group by m.id, m.name
            """;

    // language=SQL
    public static final String INSERT = """
            insert into merchants (user_id, name, city, state, postal_code, country)
            values (:userId, :name, :city, :state, :postalCode, :country)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update merchants
            set name = :name,
                city = :city,
                state = :state,
                postal_code = :postalCode,
                country = :country,
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
