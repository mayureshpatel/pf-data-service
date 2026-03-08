package com.mayureshpatel.pfdataservice.repository.transaction.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TransactionQueries {

    /**
     * Comma-separated list of aliased columns for fully hydrated transaction queries.
     * Covers transaction, account (with type), category (with parent), and merchant.
     */
    public static final String ENRICHED_COLUMNS =
            """
                    transactions.id,
                    transactions.amount,
                    transactions.date,
                    transactions.post_date,
                    transactions.description,
                    transactions.type,
                    transactions.account_id,
                    transactions.category_id,
                    transactions.merchant_id,
                    transactions.created_at,
                    transactions.updated_at,
                    transactions.deleted_at,
                    accounts.name as account_name,
                    accounts.current_balance as account_balance,
                    accounts.currency_code as accoun_currency_code,
                    accounts.bank_name as account_bank_name,
                    accounts.user_id as account_user_id,
                    accounts.version as account_version,
                    account_types.code as account_type_code,
                    account_types.label as account_type_label,
                    account_types.icon as account_type_icon,
                    account_types.color as account_type_color,
                    account_types.is_asset as account_type_is_asset,
                    account_types.sort_order as account_type_sort_order,
                    account_types.is_active as account_type_is_active,
                    categories.name as category_name,
                    categories.color as category_color,
                    categories.icon as category_icon,
                    categories.type as category_type,
                    categories.user_id as category_user_id,
                    parent_categories.id as category_parent_id,
                    parent_categories.name as category_parent_name,
                    parent_categories.color as category_parent_color,
                    parent_categories.icon as category_parent_icon,
                    parent_categories.type as category_parent_type,
                    merchants.original_name as merchant_original_name,
                    merchants.clean_name as merchant_clean_name,
                    merchants.user_id as merchant_user_id
                    """;

    /**
     * JOIN clauses that hydrate account (with type), category (with parent), and merchant.
     */
    public static final String ENRICHED_JOINS =
            "join accounts ON transactions.account_id = accounts.id " +
                    "left join account_types ON accounts.type = account_types.code " +
                    "left join categories ON transactions.category_id = categories.id " +
                    "left join categories ON categories.parent_id = parent_categories.id " +
                    "left join merchants ON transactions.merchant_id = merchants.id";

    // language=SQL
    public static final String FIND_BY_ID_WITH_DETAILS =
            "select " + ENRICHED_COLUMNS + " from transactions " + ENRICHED_JOINS +
                    " where transactions.id = :id and accounts.user_id = :userId and transactions.deleted_at is null";

    // language=SQL
    public static final String FIND_ALL_BY_IDS_WITH_DETAILS =
            "select " + ENRICHED_COLUMNS + " from transactions " + ENRICHED_JOINS +
                    " where transactions.id in (:ids) and accounts.user_id = :userId and transactions.deleted_at is null";

    // language=SQL
    public static final String FIND_BY_ID = """
            select transactions.*
            from transactions
            join accounts on transactions.account_id = accounts.id
            where transactions.id = :id
              and accounts.user_id = :userId
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from transactions
            where deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USER_ID = """
            select transactions.*
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.deleted_at is null
            order by transactions.date desc
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
              and account_id in (select id from accounts where user_id = :userId)
              and deleted_at is null
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            update transactions
            set deleted_at = CURRENT_TIMESTAMP
            where id = :id
              and account_id in (select id from accounts where user_id = :userId)
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
            select tags.*
            from transaction_tags
                left join tags on transaction_tags.tag_id = tags.id
                left join transactions on transaction_tags.transaction_id = transactions.id
            where transaction_tags.transaction_id = :transactionId
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
            select categories.id as category_id,
                   categories.name as category_name,
                   categories.color as category_color,
                   categories.icon as category_icon,
                   categories.type as category_type,
                   categories.parent_id as category_parent_id,
                   sum(transactions.amount) as total
            from transactions
            left join categories on transactions.category_id = categories.id
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.date between :startDate and :endDate
              and transactions.type = 'EXPENSE'
              and transactions.deleted_at is null
            group by categories.id
            order by total desc
            """;

    // language=SQL
    public static final String FIND_MONTHLY_SUMS = """
            select extract(year from transactions.date)  as year,
                   extract(month from transactions.date) as month,
                   transactions.type,
                   sum(transactions.amount)              as total
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
                and transactions.date >= :startDate
              and transactions.type in ('INCOME', 'EXPENSE')
              and transactions.deleted_at is null
            group by extract(year from transactions.date), extract(month from transactions.date), transactions.type
            order by year, month
            """;

    // language=SQL
    public static final String GET_MONTHLY_SPENDING = """
            select extract(year from transactions.date) as year,
                   extract(month from transactions.date) as month,
                   sum(transactions.amount) as total
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.date >= :startDate
              and transactions.type = 'EXPENSE'
              and transactions.deleted_at is null
            group by year, month
            order by year, month
            """;

    // language=SQL
    public static final String GET_SUM_BY_DATE_RANGE = """
            select coalesce(sum(transactions.amount), 0)
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.date between :startDate and :endDate
              and transactions.type = :type
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_RECENT_NON_TRANSFER = """
            select transactions.*
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.date >= :startDate
              and transactions.type not in ('TRANSFER_IN', 'TRANSFER_OUT', 'TRANSFER')
              and transactions.deleted_at is null
            order by transactions.date desc
            """;

    // language=SQL
    public static final String FIND_ALL_BY_IDS = """
            select transactions.*
            from transactions
            where transactions.id in (:ids)
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String GET_NET_FLOW_AFTER_DATE = """
            select coalesce(sum(
                case
                    when transactions.type in ('INCOME', 'TRANSFER_IN') then transactions.amount
                    when transactions.type in ('EXPENSE', 'TRANSFER_OUT') then -transactions.amount
                    else transactions.amount
                end
            ), 0)
            from transactions
            where transactions.account_id = :accountId
              and transactions.date > :date
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_EXPENSES_SINCE = """
            select transactions.*
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.date >= :startDate
              and transactions.type = 'EXPENSE'
              and transactions.deleted_at is null
            order by transactions.date desc
            """;

    // language=SQL
    public static final String GET_UNCATEGORIZED_EXPENSE_TOTALS = """
            select coalesce(sum(transactions.amount), 0)
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
              and transactions.category_id is null
              and transactions.type = 'EXPENSE'
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String COUNT_BY_CATEGORY = """
            select categories.id        as category_id,
                   categories.name      as category_name,
                   categories.color     as category_color,
                   categories.icon      as category_icon,
                   categories.type      as category_type,
                   parent_categories.id       as cateogry_parent_id,
                   parent_categories.name     as cateogry_parent_name,
                   parent_categories.color    as cateogry_parent_color,
                   parent_categories.icon     as cateogry_parent_icon,
                   parent_categories.type     as cateogry_parent_type,
                   count(transactions.id) as transaction_count
            from transactions
            join accounts on transactions.account_id = accounts.id
            join categories on transactions.category_id = categories.id
            left join categories as parent_categories on categories.parent_id = parent_categories.id
            where accounts.user_id = :userId
              and transactions.deleted_at is null
            group by categories.id,
                     categories.name,
                     categories.color,
                     categories.icon,
                     categories.type,
                     parent_categories.id,
                     parent_categories.name,
                     parent_categories.color,
                     parent_categories.icon,
                     parent_categories.type
            order by transaction_count desc
            """;

    //language=SQL
    public static final String CATEGORIES_WITH_TRANSACTIONS = """
            select distinct
                categories.*
            from transactions
            left join accounts on transactions.account_id = accounts.id
            left join categories on transactions.category_id = categories.id
            where accounts.user_id = :userId
                and transactions.deleted_at is null
                and categories.parent_id is not null
            order by categories.name
            """;

    //language=SQL
    public static final String MERCHANTS_WITH_TRANSACTIONS = """
            select distinct
                merchants.*
            from transactions
            left join accounts on transactions.account_id = accounts.id
            left join merchants on transactions.merchant_id = merchants.id
            where accounts.user_id = :userId
                and transactions.deleted_at is null
            order by merchants.clean_name
            """;
}
