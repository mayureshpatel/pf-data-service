package com.mayureshpatel.pfdataservice.repository.recurring_history.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecurringTransactionQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from recurring_transactions
            where id = :id
                and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID_ACTIVE_ORDER_BY_NEXT_DATE = """
            select *
            from recurring_transactions
            where user_id = :userId
                and active = true
                and deleted_at is null
            order by next_date
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from recurring_transactions
            where user_id = :userId
                and active = true
                and deleted_at is null
            order by merchant_id
            """;

    // language=SQL
    public static final String INSERT = """
            insert into recurring_transactions (
                                                id,
                                                user_id,
                                                account_id,
                                                merchant_id,
                                                amount,
                                                frequency,
                                                last_date,
                                                next_date,
                                                active,
                                                created_at, updated_at)
            values (
                    :id,
                    :userId,
                    :accountId,
                    :merchantName,
                    :amount,
                    :frequency,
                    :lastDate,
                    :nextDate,
                    :active,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    // language=SQL
    public static final String UPDATE = """
            update recurring_transactions
            set account_id = :accountId,
                merchant_id = :merchantId,
                amount = :amount,
                frequency = :frequency,
                last_date = :lastDate,
                next_date = :nextDate,
                active = :active,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
                and user_id = :userId
            """;

    // language=SQL
    public static final String DELETE = """
            delete from recurring_transactions
            where id = :id
                and user_id = :userId
            """;
}
