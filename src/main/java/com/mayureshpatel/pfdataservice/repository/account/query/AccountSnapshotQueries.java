package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AccountSnapshotQueries {

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE = """
            select *
            from account_snapshots
            where account_id = :accountId
              and snapshot_date = :snapshotDate
            """;

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from account_snapshots
            where id = :id
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
