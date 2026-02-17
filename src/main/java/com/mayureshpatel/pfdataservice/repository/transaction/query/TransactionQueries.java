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
    public static final String INSERT = """
            insert into transactions
                (amount, date, post_date, description, original_vendor_name, vendor_name, type, account_id, category_id, created_at, updated_at)
            values (
                    :amount,
                    :date,
                    :postDate,
                    :description,
                    :originalVendorName,
                    :vendorName,
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
                original_vendor_name = :originalVendorName,
                vendor_name = :vendorName,
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
            select coalesce(c.name, 'Uncategorized') as category_name,
                   sum(t.amount) as total
            from transactions t
            left join categories c on t.category_id = c.id
            join accounts a on t.account_id = a.id
            where a.user_id = :userId
              and t.date between :startDate and :endDate
              and t.type = 'EXPENSE'
              and t.deleted_at is null
            group by c.name
            order by total desc
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
