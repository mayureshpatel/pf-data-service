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
    public static final String FIND_MERCHANT_TOTALS = """
            select m.id as merchant_id,
                   m.original_name as merchant_original_name,
                   m.clean_name as merchant_clean_name,
                   sum(t.amount) as total
            from transactions t
            join accounts a on t.account_id = a.id
            join merchants m on t.merchant_id = m.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.date < :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by m.id
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
            """;
}
