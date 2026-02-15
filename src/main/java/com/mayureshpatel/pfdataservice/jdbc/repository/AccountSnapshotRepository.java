package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.AccountSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountSnapshotRepository implements JdbcRepository<AccountSnapshot, Long> {

    private final JdbcClient jdbcClient;
    private final SqlLoader sqlLoader;

    /**
     * Fetches account snapshot by account ID and snapshot date
     * @param accountId the account ID
     * @param snapshotDate the snapshot date
     * @return the account snapshot if found, otherwise empty
     */
    public Optional<AccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate) {
        String query = sqlLoader.load("sql/account-snapshot/by-id-and-snapshot-date.sql");
        return jdbcClient.sql(query)
                .param("accountId", accountId)
                .param("snapshotDate", snapshotDate)
                .query(AccountSnapshot.class)
                .optional();
    }
}
