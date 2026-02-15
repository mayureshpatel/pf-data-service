package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor( access = lombok.AccessLevel.PRIVATE)
public final class AccountSnapshotQueries {

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE = """
            select *
            from account_snapshots
            where id = :id
              and snapshot_date = :snapshotDate
            """;
}
