package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.SqlParams;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed persistence for {@link Account}. The explicit {@code "jdbcAccountRepository"} bean
 * name isn't currently required by any {@code @Qualifier}/{@code @Resource} lookup in this
 * codebase (every injection site resolves it by type, since it's the only bean of this type) --
 * kept defensively rather than confirmed as load-bearing.
 */
@Repository("jdbcAccountRepository")
@RequiredArgsConstructor
public class AccountRepository implements JdbcRepository<Account, Long>, SoftDeleteSupport {

    private final JdbcClient jdbcClient;
    private final AccountRowMapper rowMapper;

    @Override
    public List<Account> findAll() {
        return jdbcClient.sql(AccountQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jdbcClient.sql(AccountQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param userId the user id
     * @return every non-deleted account owned by the user
     */
    public List<Account> findAllByUserId(Long userId) {
        return jdbcClient.sql(AccountQueries.FIND_ALL_BY_USER_ID)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .list();
    }

    /**
     * Ownership-scoped lookup -- the standard pattern this codebase uses to make sure a caller
     * can never fetch an account they don't own by guessing/brute-forcing its id.
     *
     * @param accountId the account id
     * @param userId    the requesting user's id
     * @return the account if it exists and is owned by {@code userId}, otherwise empty
     */
    public Optional<Account> findByIdAndUserId(Long accountId, Long userId) {
        return jdbcClient.sql(AccountQueries.FIND_BY_ACCOUNT_ID_AND_USER_ID)
                .param("accountId", accountId)
                .param(SqlParams.USER_ID, userId)
                .query(rowMapper)
                .optional();
    }

    /**
     * @param userId  the owning user's id
     * @param request the account details to create
     * @return the generated account id
     */
    public int insert(Long userId, AccountCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(AccountQueries.INSERT)
                .param("name", request.getName())
                .param("type", request.getType())
                .param("currentBalance", request.getStartingBalance())
                .param("currencyCode", request.getCurrencyCode())
                .param("bankName", request.getBankName())
                .param(SqlParams.USER_ID, userId)
                .param("createdBy", userId)
                .param("updatedBy", userId)
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * Updates an account's editable fields. Ownership- and version-scoped in the underlying SQL
     * ({@code AccountQueries.UPDATE}'s {@code where} clause) -- a stale {@code request.version}
     * or a mismatched {@code userId} silently updates zero rows rather than throwing, unlike
     * {@link #reconcile}/{@link #updateBalance} below, which explicitly reject that case.
     *
     * @param userId  the requesting user's id
     * @param request the fields to update, including the optimistic-locking {@code version}
     * @return the number of rows updated (0 or 1)
     */
    public int update(Long userId, AccountUpdateRequest request) {
        return jdbcClient.sql(AccountQueries.UPDATE)
                .param("name", request.getName())
                .param("type", request.getType())
                .param("currencyCode", request.getCurrencyCode())
                .param("bankName", request.getBankName())
                .param("updatedBy", userId)
                .param("id", request.getId())
                .param("version", request.getVersion())
                .update();
    }

    @Override
    public int deleteById(Long id, Long userId) {
        return jdbcClient.sql(AccountQueries.DELETE_BY_ID)
                .param("id", id)
                .param("deletedBy", userId)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(AccountQueries.COUNT_ACTIVE)
                .query(Long.class)
                .single();
    }

    /**
     * Sets an account's balance directly to {@code targetBalance} (as opposed to
     * {@link #updateBalance}, which is always a relative change applied by a transaction).
     * Optimistic-locked on {@code version}: a conflicting concurrent modification throws rather
     * than silently overwriting it, so the caller can decide whether to retry against fresh data
     * (see {@code AccountService#reconcileAccount}'s own Javadoc for why this is deliberate).
     *
     * @param userId        the requesting user's id
     * @param accountId     the account to reconcile
     * @param targetBalance the known-correct balance to set
     * @param version       the optimistic-locking version last read by the caller
     * @return the number of rows updated (always 1 on success)
     * @throws OptimisticLockingFailureException if the account was concurrently modified
     */
    public int reconcile(Long userId, Long accountId, BigDecimal targetBalance, Long version) {
        int updated = jdbcClient.sql(AccountQueries.RECONCILE)
                .param("accountId", accountId)
                .param(SqlParams.USER_ID, userId)
                .param("targetBalance", targetBalance)
                .param("version", version)
                .update();

        if (updated == 0) {
            throw new OptimisticLockingFailureException("Account reconciliation failed due to concurrent modification");
        }
        return updated;
    }

    /**
     * Persists an already-computed new balance for an account (the caller has already applied a
     * transaction's net change -- this method doesn't do that arithmetic itself). Optimistic-locked
     * on {@code version} the same way as {@link #reconcile}.
     *
     * @param userId        the requesting user's id
     * @param accountId     the account to update
     * @param currentBalance the new balance to persist
     * @param version       the optimistic-locking version last read by the caller
     * @return the number of rows updated (always 1 on success)
     * @throws OptimisticLockingFailureException if the account was concurrently modified
     */
    public int updateBalance(Long userId, Long accountId, BigDecimal currentBalance, Long version) {
        int updated = jdbcClient.sql(AccountQueries.UPDATE_BALANCE)
                .param("accountId", accountId)
                .param(SqlParams.USER_ID, userId)
                .param("currentBalance", currentBalance)
                .param("version", version)
                .update();
                
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }
        return updated;
    }
}
