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
    public static final String FIND_ALL_BY_CLEAN_NAME = """
            select *
            from merchants
            where clean_name = :cleanName
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAME_LIKE = """
            select *
            from merchants
            where clean_name like :cleanName
            """;

    // language=SQL
    public static final String FIND_MERCHANT_TOTALS = """
            select m.id as merchant_id,
                   m.original_name as merchant_original_name,
                   m.clean_name as merchant_clean_name,
                   sum(t.amount) as total
            from transactions t
            join accounts a on t.account_id = a.id
            join merchants m on t.merchant_id = m.id
            where a.user_id = :userId
              and extract(month from t.date) = :month
              and extract(year from t.date) = :year
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by m.id
            """;
}
