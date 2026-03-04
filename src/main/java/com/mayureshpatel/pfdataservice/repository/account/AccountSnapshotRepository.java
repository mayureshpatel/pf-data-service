package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountSnapshotQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountSnapshotRepository implements JdbcRepository<AccountSnapshot, Long> {

    private final JdbcClient jdbcClient;

    /**
     * Fetches account snapshot by account ID and snapshot date
     *
     * @param accountId    the account ID
     * @param snapshotDate the snapshot date
     * @return the account snapshot if found, otherwise empty
     */
    public Optional<AccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE)
                .param("accountId", accountId)
                .param("snapshotDate", snapshotDate)
                .query(AccountSnapshot.class)
                .optional();
    }

    @Override
    public Optional<AccountSnapshot> findById(Long id) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_BY_ID)
                .param("id", id)
                .query(AccountSnapshot.class)
                .optional();
    }

    @Override
    public int insert(AccountSnapshot entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long accId = entity.getAccountId();
        if (accId == null && entity.getAccount() != null) {
            accId = entity.getAccount().getId();
        }

        return jdbcClient.sql(AccountSnapshotQueries.INSERT)
                .param("accountId", accId)
                .param("snapshotDate", entity.getSnapshotDate())
                .param("balance", entity.getBalance())
                .update(keyHolder);
    }

    @Override
    public int update(AccountSnapshot entity) {
        return jdbcClient.sql(AccountSnapshotQueries.UPDATE)
                .param("balance", entity.getBalance())
                .param("id", entity.getId())
                .update();
    }

    @Override
    public int delete(AccountSnapshot entity) {
        if (entity.getId() != null) {
            return deleteById(entity.getId());
        }
        return 0;
    }

    @Override
    public int deleteById(Long id) {
        return jdbcClient.sql(AccountSnapshotQueries.DELETE_BY_ID)
                .param("id", id)
                .update();
    }
}
