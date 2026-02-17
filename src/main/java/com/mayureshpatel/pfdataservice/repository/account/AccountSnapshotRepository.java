package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountSnapshotQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountSnapshotRepository implements JdbcRepository<AccountSnapshot, Long> {

    private final JdbcClient jdbcClient;

    /**
     * Fetches account snapshot by account ID and snapshot date
     * @param accountId the account ID
     * @param snapshotDate the snapshot date
     * @return the account snapshot if found, otherwise empty
     */
    public Optional<AccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE)
                .param("id", accountId)
                .param("snapshotDate", snapshotDate)
                .query(AccountSnapshot.class)
                .optional();
    }

    @Override
    public AccountSnapshot save(AccountSnapshot entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    @Override
    public void delete(AccountSnapshot entity) {
        if (entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
}
