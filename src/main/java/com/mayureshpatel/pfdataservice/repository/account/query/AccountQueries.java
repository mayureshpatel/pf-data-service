package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

/** SQL query constants for {@code AccountRepository}. */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AccountQueries {

    /**
     * Common SELECT columns for accounts with enriched metadata (type, currency).
     */
    private static final String ENRICHED_COLUMNS = """
            select accounts.*,
                   account_types.code as account_type_code,
                   account_types.label as account_type_label,
                   account_types.color as account_type_color,
                   account_types.icon as account_type_icon,
                   account_types.is_asset as account_type_is_asset,
                   currencies.code as currency_code,
                   currencies.name as currency_name,
                   currencies.symbol as currency_symbol
            """;

    private static final String ENRICHED_FROM = """
            from accounts
                left join account_types on accounts.type = account_types.code
                left join currencies on accounts.currency_code = currencies.code
            """;

    private static final String ENRICHED_SELECT = ENRICHED_COLUMNS + ENRICHED_FROM;

    /**
     * The enriched select plus, for hand-entered accounts, the net money in (contributions less
     * transfers out and expenses, deleted rows excluded) and the newest recorded balance. The money
     * in is one grouped aggregate joined in, not a query per account; earnings, employer
     * contributions and every other type are deliberately left out of it.
     */
    private static final String ENRICHED_WITH_MANUAL_FIGURES = ENRICHED_COLUMNS + """
                   , case when accounts.balance_mode = 'MANUAL' then coalesce(money_in.total, 0) end as money_in,
                   case when accounts.balance_mode = 'MANUAL' then
                       (select s.balance from account_snapshots s
                        where s.account_id = accounts.id
                        order by s.snapshot_date desc limit 1)
                   end as latest_recorded_balance
            """ + ENRICHED_FROM + """
                left join (
                    select t.account_id,
                           sum(case when t.type = 'CONTRIBUTION' then t.amount
                                    when t.type in ('TRANSFER_OUT', 'EXPENSE') then -t.amount
                                    else 0 end) as total
                    from transactions t
                    where t.deleted_at is null
                    group by t.account_id
                ) money_in on money_in.account_id = accounts.id
            """;

    // language=SQL
    public static final String COUNT_ACTIVE = """
            select count(*)
            from accounts
            where deleted_at is null
            """;

    // language=SQL
    public static final String COUNT_ALL = """
            select count(*)
            from accounts
            """;

    // language=SQL
    public static final String COUNT_DELETED = """
            select count(*)
            from accounts
            where deleted_at is not null
            """;

    // language=SQL
    public static final String FIND_ALL = ENRICHED_SELECT + """
            where accounts.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_ID = ENRICHED_SELECT + """
            where accounts.id = :id
                and accounts.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_USER_ID = ENRICHED_SELECT + """
            where accounts.id = :accountId
                and accounts.user_id = :userId
                and accounts.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = ENRICHED_WITH_MANUAL_FIGURES + """
            where accounts.user_id = :userId
                and accounts.deleted_at is null
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from accounts
            where id = :id
                and deleted_at IS NULL
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            update accounts
            set deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :deletedBy
            where id = :id
                and user_id = :deletedBy
                and deleted_at is null
            """;

    // language=SQL
    public static final String INSERT = """
            insert into accounts
                (
                 name,
                 type,
                 current_balance,
                 currency_code,
                 bank_name,
                 balance_mode,
                 user_id,
                 version,
                 created_at, created_by, updated_at, updated_by
                 )
            values(
                   :name,
                   :type,
                   :currentBalance,
                   :currencyCode,
                   :bankName,
                   coalesce(cast(:balanceMode as varchar), 'DERIVED'),
                   :userId,
                   1,
                   CURRENT_TIMESTAMP, :createdBy, CURRENT_TIMESTAMP, :updatedBy
                   )
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update accounts
            set name = :name,
                type = :type,
                currency_code = :currencyCode,
                bank_name = :bankName,
                balance_mode = coalesce(cast(:balanceMode as varchar), balance_mode),
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP, updated_by = :updatedBy
            where id = :id
              and user_id = :updatedBy
              and version = :version
              and deleted_at is null
            """;

    // language=SQL
    public static final String UPDATE_BALANCE = """
            update accounts
            set current_balance = :currentBalance,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId
            where id = :accountId
              and user_id = :userId
              and version = :version
              and deleted_at is null
            """;

    // language=SQL
    public static final String RECONCILE = """
            update accounts
            set current_balance = :targetBalance,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP, updated_by = :userId
            where id = :accountId
                and user_id = :userId
                and version = :version
                and deleted_at is null
            """;
}
