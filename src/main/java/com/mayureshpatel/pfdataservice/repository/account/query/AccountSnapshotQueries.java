package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AccountSnapshotQueries {

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE = """
            select
                account_snapshots.*,
                accounts.id as account_id,
                accounts.name as account_name,
                accounts.bank_name as account_bank_name,
                account_types.code as account_type_code,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol
            from account_snapshots
            left join accounts on accounts.id = account_snapshots.account_id
            left join account_types on account_types.code = accounts.type
            left join currencies on currencies.code = accounts.currency_code
            where accounts.id = :accountId
              and account_snapshots.snapshot_date = :snapshotDate
            """;

    // language=SQL
    public static final String FIND_BY_ID = """
            select
                account_snapshots.*,
                accounts.id as account_id,
                accounts.name as account_name,
                accounts.bank_name as account_bank_name,
                account_types.code as account_type_code,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol
            from account_snapshots
            left join accounts on accounts.id = account_snapshots.account_id
            left join account_types on account_types.code = accounts.type
            left join currencies on currencies.code = accounts.currency_code
            where account_snapshots.id = :id
            """;

    // language=SQL
    public static final String INSERT = """
            insert into account_snapshots (account_id, snapshot_date, balance, created_at)
            values (:accountId, :snapshotDate, :balance, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update account_snapshots
            set balance = :balance
            where id = :id
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            delete from account_snapshots
            where id = :id
            """;
}
