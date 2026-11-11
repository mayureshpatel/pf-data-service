package com.mayureshpatel.pfdataservice.repository.transaction.query;

import com.mayureshpatel.pfdataservice.service.DescriptionKeys;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SQL query constants for {@code TransactionRepository} -- by far this codebase's largest query
 * class, backing everything from basic CRUD to the dashboard pulse widget and the Reports feature's
 * category/cash-flow breakdowns. {@link #ENRICHED_COLUMNS}/{@link #ENRICHED_JOINS} are shared
 * building blocks composed into most of the {@code FIND_*} queries below that need a fully
 * hydrated transaction (account, category, merchant all joined in), rather than every such query
 * repeating the same column list and joins independently.
 */
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
                    transactions.date as transaction_date,
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
                    accounts.type as account_type,
                    accounts.current_balance as account_current_balance,
                    accounts.currency_code as account_currency_code,
                    accounts.bank_name as account_bank_name,
                    accounts.user_id as account_user_id,
                    accounts.version as account_version,
                    account_types.code as account_account_type_code,
                    account_types.label as account_account_type_label,
                    account_types.icon as account_account_type_icon,
                    account_types.color as account_account_type_color,
                    account_types.is_asset as account_account_type_is_asset,
                    account_types.sort_order as account_account_type_sort_order,
                    account_types.is_active as account_account_type_is_active,
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
                    merchants.name as merchant_name,
                    merchants.city as merchant_city,
                    merchants.state as merchant_state,
                    merchants.postal_code as merchant_postal_code,
                    merchants.country as merchant_country,
                    merchants.user_id as merchant_user_id
                    """;

    /**
     * JOIN clauses that hydrate account (with type), category (with parent), and merchant.
     */
    public static final String ENRICHED_JOINS =
            "join accounts ON transactions.account_id = accounts.id " +
                    "left join account_types ON accounts.type = account_types.code " +
                    "left join categories ON transactions.category_id = categories.id " +
                    "left join categories parent_categories ON categories.parent_id = parent_categories.id " +
                    "left join merchants ON transactions.merchant_id = merchants.id";

    /**
     * Concatenation fragments shared by the {@code FIND_*} queries below that build their SQL by
     * composing {@link #ENRICHED_COLUMNS}/{@link #ENRICHED_JOINS} with a query-specific {@code
     * where} clause, rather than a self-contained text block.
     */
    private static final String SELECT = "select ";
    private static final String FROM_TRANSACTIONS = " from transactions ";
    private static final String WHERE_USER_OWNS_ACCOUNT = " where accounts.user_id = :userId";
    private static final String AND_NOT_DELETED = "   and transactions.deleted_at is null";

    /** Name prefix of the generated keyword parameters ({@code k0}, {@code k1}, ...). */
    public static final String CANDIDATE_KEYWORD_PARAM_PREFIX = "k";

    // language=SQL
    // PF-308: batch-fetches tags for a whole page of transactions in one query (not per-row),
    // assembled onto each Transaction in Java by TransactionRepository.findAll -- same pattern as
    // CategoryRuleRepository's keyword-set assembly (PF-315).
    public static final String FIND_TAGS_BY_TRANSACTION_IDS = """
            select transaction_tags.transaction_id, tags.*
            from transaction_tags
                join tags on transaction_tags.tag_id = tags.id
            where transaction_tags.transaction_id in (:transactionIds)
            """;

    // language=SQL
    public static final String FIND_BY_ID_WITH_DETAILS =
            SELECT + ENRICHED_COLUMNS + FROM_TRANSACTIONS + ENRICHED_JOINS +
                    " where transactions.id = :id and accounts.user_id = :userId and transactions.deleted_at is null";

    // language=SQL
    public static final String FIND_ALL_BY_IDS_WITH_DETAILS =
            SELECT + ENRICHED_COLUMNS + FROM_TRANSACTIONS + ENRICHED_JOINS +
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
    public static final String FIND_BY_USER_ID = SELECT + ENRICHED_COLUMNS +
            FROM_TRANSACTIONS + ENRICHED_JOINS +
            WHERE_USER_OWNS_ACCOUNT +
            AND_NOT_DELETED +
            " order by transactions.date desc";

    /**
     * Builds the PF-900 candidate-prefilter query: the same select, joins, ownership scope and
     * soft-delete exclusion as {@link #FIND_BY_USER_ID}, restricted to rows whose lowercased
     * description matches any of {@code keywordCount} bound {@code like} patterns. Only the
     * generated parameter names ({@code :k0}, {@code :k1}, ...) are ever placed in the SQL text;
     * the keyword values themselves are bound separately, so user input is never concatenated.
     *
     * @param keywordCount how many {@code :kN} predicates to generate; must be at least 1
     * @return the sql with one {@code lower(description) like :kN escape '\'} predicate per keyword
     */
    public static String findCandidatesByDescriptionKeywords(int keywordCount) {
        String predicates = IntStream.range(0, keywordCount)
                .mapToObj(i -> "lower(transactions.description) like :"
                        + CANDIDATE_KEYWORD_PARAM_PREFIX + i + " escape '\\'")
                .collect(Collectors.joining(" or "));
        return SELECT + ENRICHED_COLUMNS + FROM_TRANSACTIONS + ENRICHED_JOINS +
                WHERE_USER_OWNS_ACCOUNT + AND_NOT_DELETED +
                " and (" + predicates + ") order by transactions.date desc";
    }

    // language=SQL
    // PF-907: the merchant-less candidates for applying merchant rules; same scope as FIND_BY_USER_ID
    public static final String FIND_MERCHANTLESS_BY_USER_ID = SELECT + ENRICHED_COLUMNS +
            FROM_TRANSACTIONS + ENRICHED_JOINS +
            WHERE_USER_OWNS_ACCOUNT +
            AND_NOT_DELETED +
            "   and transactions.merchant_id is null" +
            " order by transactions.date desc, transactions.id desc";

    /** Name prefix of the generated transaction id parameters of the merchant batch update. */
    public static final String ASSIGN_ID_PARAM_PREFIX = "tx";

    /** Name prefix of the generated merchant id parameters of the merchant batch update. */
    public static final String ASSIGN_MERCHANT_PARAM_PREFIX = "mx";

    /**
     * Builds the PF-907 batch update that sets {@code merchant_id} for many rows in one statement.
     * Each row only changes when it is still merchant-less, not deleted and owned by the user, so
     * a merchant set by a concurrent edit is never overwritten. Only generated parameter names
     * ({@code :tx0}/{@code :mx0}, ...) are placed in the sql text; the values are bound separately.
     *
     * @param pairCount how many (transaction id, merchant id) pairs to generate; must be at least 1
     * @return the update sql
     */
    public static String assignMerchantsChunk(int pairCount) {
        String pairs = IntStream.range(0, pairCount)
                .mapToObj(i -> "(cast(:" + ASSIGN_ID_PARAM_PREFIX + i + " as bigint), cast(:"
                        + ASSIGN_MERCHANT_PARAM_PREFIX + i + " as bigint))")
                .collect(Collectors.joining(", "));
        return "update transactions set merchant_id = v.merchant_id, updated_at = CURRENT_TIMESTAMP"
                + " from (values " + pairs + ") as v(id, merchant_id)"
                + " where transactions.id = v.id"
                + " and transactions.merchant_id is null"
                + " and transactions.deleted_at is null"
                + " and transactions.account_id in (select id from accounts where user_id = :userId)";
    }

    /**
     * PF-920 overwrite of the category on chosen transactions, in one statement. A row changes only
     * if it is owned by the user and not deleted; unlike {@link #assignMerchantsChunk} it does not
     * require the row to be uncategorized, because the user chose these rows after seeing a preview.
     */
    // language=SQL
    public static final String OVERWRITE_CATEGORY = """
            update transactions
            set category_id = :categoryId, updated_at = CURRENT_TIMESTAMP
            where id in (:ids)
              and deleted_at is null
              and account_id in (select id from accounts where user_id = :userId)
            """;

    /**
     * PF-921 overwrite of the merchant on chosen transactions, in one statement. A row changes only
     * if it is owned by the user and not deleted; unlike {@link #assignMerchantsChunk} it does not
     * require the row to be merchant-less, because the user chose these rows after seeing a preview.
     */
    // language=SQL
    public static final String OVERWRITE_MERCHANT = """
            update transactions
            set merchant_id = :merchantId, updated_at = CURRENT_TIMESTAMP
            where id in (:ids)
              and deleted_at is null
              and account_id in (select id from accounts where user_id = :userId)
            """;

    /**
     * PF-922 undo of {@link #OVERWRITE_CATEGORY}: puts a previous category (possibly none) back on
     * chosen transactions, but only on rows that still carry the category the apply set, so a row
     * changed since is never overwritten. Scoped to the user's non-deleted transactions.
     */
    // language=SQL
    public static final String RESTORE_CATEGORY = """
            update transactions
            set category_id = cast(:previousId as bigint), updated_at = CURRENT_TIMESTAMP
            where id in (:ids)
              and category_id = :appliedId
              and deleted_at is null
              and account_id in (select id from accounts where user_id = :userId)
            """;

    /**
     * PF-922 undo of {@link #OVERWRITE_MERCHANT}: puts a previous merchant (possibly none) back on
     * chosen transactions, but only on rows that still carry the merchant the apply set, so a row
     * changed since is never overwritten. Scoped to the user's non-deleted transactions.
     */
    // language=SQL
    public static final String RESTORE_MERCHANT = """
            update transactions
            set merchant_id = cast(:previousId as bigint), updated_at = CURRENT_TIMESTAMP
            where id in (:ids)
              and merchant_id = :appliedId
              and deleted_at is null
              and account_id in (select id from accounts where user_id = :userId)
            """;

    // language=SQL
    public static final String FIND_EXISTING_FOR_DUPLICATE_CHECK = """
            select *
            from transactions
            where account_id = :accountId
              and date >= :startDate
              and date <= :endDate
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
            select extract(year from transactions.date at time zone 'UTC')  as year,
                   extract(month from transactions.date at time zone 'UTC') as month,
                   transactions.type,
                   sum(transactions.amount)              as total
            from transactions
            join accounts on transactions.account_id = accounts.id
            where accounts.user_id = :userId
                and transactions.date >= :startDate
              and transactions.type in ('INCOME', 'EXPENSE')
              and transactions.deleted_at is null
            group by extract(year from transactions.date at time zone 'UTC'), extract(month from transactions.date at time zone 'UTC'), transactions.type
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
    public static final String FIND_RECENT_NON_TRANSFER = SELECT + ENRICHED_COLUMNS +
            FROM_TRANSACTIONS + ENRICHED_JOINS +
            WHERE_USER_OWNS_ACCOUNT +
            "   and transactions.date >= :startDate" +
            "   and transactions.type not in ('TRANSFER_IN', 'TRANSFER_OUT', 'TRANSFER')" +
            AND_NOT_DELETED +
            " order by transactions.date desc";

    // language=SQL
    public static final String FIND_ALL_BY_IDS = """
            select transactions.*
            from transactions
            where transactions.id in (:ids)
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String GET_NET_FLOW_FROM_DATE = """
            select coalesce(sum(
                case
                    when transactions.type in ('INCOME', 'TRANSFER_IN') then transactions.amount
                    when transactions.type in ('CONTRIBUTION', 'EMPLOYER_CONTRIBUTION', 'EARNINGS') then abs(transactions.amount)
                    when transactions.type in ('EXPENSE', 'TRANSFER_OUT') then -transactions.amount
                    else transactions.amount
                end
            ), 0)
            from transactions
                     join accounts on accounts.id = transactions.account_id
            where transactions.account_id = :accountId
              and accounts.balance_mode = 'DERIVED'
              and transactions.date >= :date
              and transactions.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_EXPENSES_SINCE = SELECT + ENRICHED_COLUMNS +
            FROM_TRANSACTIONS + ENRICHED_JOINS +
            WHERE_USER_OWNS_ACCOUNT +
            "   and transactions.date >= :startDate" +
            "   and transactions.type = 'EXPENSE'" +
            AND_NOT_DELETED +
            " order by transactions.date desc";

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
    public static final String GET_UNCATEGORIZED_EXPENSE_COUNT = """
            select count(*)
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
                   parent_categories.id       as category_parent_id,
                   parent_categories.name     as category_parent_name,
                   parent_categories.color    as category_parent_color,
                   parent_categories.icon     as category_parent_icon,
                   parent_categories.type     as category_parent_type,
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
                and merchants.id is not null
            order by merchants.name
            """;

    // language=SQL
    // PF-848: selects every TRANSFER_IN transaction on a credit-card account -- the only type of
    // row the old configureCreditCardTransactionTypeAndAmount() heuristic (fixed by PF-829) could
    // ever have produced. Scoped to credit-card accounts specifically (not "any TRANSFER_IN"), so
    // this never touches a genuine markAsTransfer()-confirmed transfer that might exist on some
    // other account type.
    public static final String FIND_TRANSFER_IN_ON_CREDIT_CARD_ACCOUNTS = SELECT + ENRICHED_COLUMNS +
            FROM_TRANSACTIONS + ENRICHED_JOINS +
            WHERE_USER_OWNS_ACCOUNT +
            "   and transactions.type = 'TRANSFER_IN'" +
            "   and accounts.type = 'CREDIT_CARD'" +
            AND_NOT_DELETED;

    // language=SQL
    // groups the user's live transaction descriptions by their light-normalized form (the key
    // merchant_description_links.normalized_description uses), showing each group's most common raw
    // spelling. the search and unlinked-only fragments below are appended by the repository.
    public static final String DESCRIPTIONS_GROUPED = """
            with grouped as (
                select ${descriptionKey} as norm_key,
                       mode() within group (order by transactions.description) as description,
                       count(*) as transaction_count
                from transactions
                    join accounts on transactions.account_id = accounts.id
                where accounts.user_id = :userId
                  and transactions.deleted_at is null
                  and ${descriptionKey} <> ''
                group by 1
            )
            """.replace("${descriptionKey}", DescriptionKeys.sqlFold("transactions.description"));

    // language=SQL
    public static final String DESCRIPTIONS_JOINED = """
            from grouped
                left join merchant_description_links links
                    on links.user_id = :userId and links.normalized_description = grouped.norm_key
                left join merchants on merchants.id = links.merchant_id
            where 1 = 1
            """;

    // language=SQL
    public static final String DESCRIPTIONS_SEARCH = " and grouped.norm_key like :search escape '\\'";

    // language=SQL
    public static final String DESCRIPTIONS_UNLINKED_ONLY = " and links.id is null";

    // language=SQL
    public static final String DESCRIPTIONS_SELECT =
            "select grouped.description, grouped.transaction_count, merchants.id as merchant_id, merchants.name as merchant_name ";

    // language=SQL
    public static final String DESCRIPTIONS_ORDER_PAGE =
            " order by grouped.transaction_count desc, grouped.description limit :limit offset :offset";
}
