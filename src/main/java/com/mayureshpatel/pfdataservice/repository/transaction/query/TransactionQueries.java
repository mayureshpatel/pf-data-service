package com.mayureshpatel.pfdataservice.repository.transaction.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TransactionQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from transactions
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from transactions
            where deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID = """
            select t.*
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.deleted_at is null
            order by t.date desc
            """;

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_DATE_AND_AMOUNT_AND_DESCRIPTION_AND_TYPE = """
            select *
            from transactions
            where account_id = :accountId
                and date = :transactionDate
                and amount = :amount
                and description = :description
                and type = :type
            """;

    // language=SQL
    public static final String INSERT = """
            insert into transactions
                (amount, date, post_date, description, merchant_id, type, account_id, category_id, created_at, updated_at)
            values (
                    :amount,
                    :date,
                    :postDate,
                    :description,
                    :merchantId,
                    :type,
                    :accountId,
                    :categoryId,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update transactions
            set amount = :amount,
                date = :date,
                post_date = :postDate,
                description = :description,
                merchant_id = :merchantId,
                type = :type,
                account_id = :accountId,
                category_id = :categoryId,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            update transactions
            set deleted_at = CURRENT_TIMESTAMP
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*)
            from transactions
            where deleted_at is null
            """;

    // language=SQL
    public static final String COUNT_BY_ACCOUNT_ID = """
            select count(*)
            from transactions
            where account_id = :accountId
              and deleted_at is null
    """;

    // language=SQL
    public static final String COUNT_BY_CATEGORY_ID = """
            select count(*)
            from transactions
            where category_id = :categoryId
              and deleted_at is null
    """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from transactions
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_TAGS_BY_TRANSACTION_ID = """
            select t.*
            from tags t
            join transaction_tags tt on t.id = tt.tag_id
            where tt.transaction_id = :transactionId
            """;

    // language=SQL
    public static final String INSERT_TRANSACTION_TAG = """
            insert into transaction_tags
                (transaction_id, tag_id)
            values (
                    :transactionId,
                    :tagId)
            """;

    // language=SQL
    public static final String DELETE_TRANSACTION_TAGS = """
            delete from transaction_tags
            where transaction_id = :transactionId
            """;

    // language=SQL
    public static final String FIND_CATEGORY_TOTALS = """
            select c.id as category_id,
                   c.name as category_name,
                   c.color as category_color,
                   c.icon as category_icon,
                   c.type as category_type,
                   c.parent_id as category_parent_id,
                   sum(t.amount) as total
            from transactions t
            left join categories c on t.category_id = c.id
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date between :startDate and :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by c.id
            order by total desc
            """;

    // language=SQL
    public static final String FIND_MONTHLY_SUMS = """
            select extract(year from t.date)  as year,
                   extract(month from t.date) as month,
                   t.type,
                   sum(t.amount)              as total
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.type in ('INCOME', 'EXPENSE')
              and t.deleted_at is null
            group by extract(year from t.date), extract(month from t.date), t.type
            order by year, month
            """;

    // language=SQL
    public static final String GET_MONTHLY_SPENDING = """
            select extract(year from t.date) as year,
                   extract(month from t.date) as month,
                   sum(t.amount) as total
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by year, month
            order by year, month
            """;

    // language=SQL
    public static final String GET_SUM_BY_DATE_RANGE = """
            select coalesce(sum(t.amount), 0)
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date between :startDate and :endDate
              and t.type = :type
              and t.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_RECENT_NON_TRANSFER = """
            select t.*
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.type not in ('TRANSFER_IN', 'TRANSFER_OUT', 'TRANSFER')
              and t.deleted_at is null
            order by t.date desc
            """;

    // language=SQL
    public static final String FIND_ALL_BY_IDS = """
            select t.*
            from transactions t
            where t.id in (:ids)
              and t.deleted_at is null
            """;

    // language=SQL
    public static final String GET_NET_FLOW_AFTER_DATE = """
            select coalesce(sum(
                case
                    when t.type in ('INCOME', 'TRANSFER_IN') then t.amount
                    when t.type in ('EXPENSE', 'TRANSFER_OUT') then -t.amount
                    else t.amount
                end
            ), 0)
            from transactions t
            where t.account_id = :accountId
              and t.date > :date
              and t.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_EXPENSES_SINCE = """
            select t.*
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date >= :startDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            order by t.date desc
            """;

    // language=SQL
    public static final String GET_UNCATEGORIZED_EXPENSE_TOTALS = """
            select coalesce(sum(t.amount), 0)
            from transactions t
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.category_id is null
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            """;
}
