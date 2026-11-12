package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountSnapshotRowMapper;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountSnapshotQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** JDBC-backed persistence for {@link AccountSnapshot}. */
@Repository
@RequiredArgsConstructor
public class AccountSnapshotRepository implements JdbcRepository<AccountSnapshot, Long> {

    private static final String ACCOUNT_ID_PARAM = "accountId";

    private final JdbcClient jdbcClient;
    private final AccountSnapshotRowMapper rowMapper;

    /**
     * Fetches account snapshot by account ID and snapshot date
     *
     * @param accountId    the account ID
     * @param snapshotDate the snapshot date
     * @return the account snapshot if found, otherwise empty
     */
    public Optional<AccountSnapshot> findByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_BY_ACCOUNT_ID_AND_SNAPSHOT_DATE)
                .param(ACCOUNT_ID_PARAM, accountId)
                .param("snapshotDate", snapshotDate)
                .query(rowMapper)
                .optional();
    }

    /**
     * Inserts the balance for an account and date, or replaces it when one already exists
     * (the table's unique key is account + date, so a second entry for a date must not fail).
     *
     * @param accountId    the account ID
     * @param snapshotDate the date the balance was true on
     * @param balance      the balance to store
     * @return the id of the inserted or replaced snapshot
     */
    public int upsert(Long accountId, LocalDate snapshotDate, BigDecimal balance) {
        return jdbcClient.sql(AccountSnapshotQueries.UPSERT)
                .param(ACCOUNT_ID_PARAM, accountId)
                .param("snapshotDate", snapshotDate)
                .param("balance", balance)
                .query(Long.class)
                .single()
                .intValue();
    }

    /**
     * Lists every snapshot for an account, newest first.
     *
     * @param accountId the account ID
     * @return the snapshots by date descending, empty if the account has none
     */
    public List<AccountSnapshot> findAllByAccountIdNewestFirst(Long accountId) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_ALL_BY_ACCOUNT_ID_NEWEST_FIRST)
                .param(ACCOUNT_ID_PARAM, accountId)
                .query(rowMapper)
                .list();
    }

    /**
     * Finds the date of the account's newest snapshot.
     *
     * @param accountId the account ID
     * @return the newest snapshot date, or empty if the account has no snapshots
     */
    public Optional<LocalDate> findLatestSnapshotDate(Long accountId) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_LATEST_SNAPSHOT_DATE)
                .param(ACCOUNT_ID_PARAM, accountId)
                .query((rs, rowNum) -> rs.getObject(1, LocalDate.class))
                .optional();
    }

    @Override
    public Optional<AccountSnapshot> findById(Long id) {
        return jdbcClient.sql(AccountSnapshotQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    @Override
    public int insert(AccountSnapshot entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long accId = entity.getAccountId();
        if (accId == null && entity.getAccount() != null) {
            accId = entity.getAccount().getId();
        }

        jdbcClient.sql(AccountSnapshotQueries.INSERT)
                .param(ACCOUNT_ID_PARAM, accId)
                .param("snapshotDate", entity.getSnapshotDate())
                .param("balance", entity.getBalance())
                .update(keyHolder);

        return keyHolder.getKey().intValue();
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
