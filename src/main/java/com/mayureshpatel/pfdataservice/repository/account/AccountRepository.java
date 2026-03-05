package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.account.AccountCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
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

    public List<Account> findAllByUserId(Long userId) {
        return jdbcClient.sql(AccountQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    public Optional<Account> findByIdAndUserId(Long accountId, Long userId) {
        return jdbcClient.sql(AccountQueries.FIND_BY_ACCOUNT_ID_AND_USER_ID)
                .param("accountId", accountId)
                .param("userId", userId)
                .query(rowMapper)
                .optional();
    }

    public int insert(Long userId, AccountCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        return jdbcClient.sql(AccountQueries.INSERT)
                .param("name", request.getName())
                .param("type", request.getType())
                .param("currentBalance", request.getStartingBalance())
                .param("currencyCode", request.getCurrencyCode())
                .param("bankName", request.getBankName())
                .param("userId", userId)
                .param("createdBy", userId)
                .param("updatedBy", userId)
                .update(keyHolder);
    }

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

    public int reconcile(Long userId, Long accountId, BigDecimal targetBalance, Long version) {
        int updated = jdbcClient.sql(AccountQueries.RECONCILE)
                .param("accountId", accountId)
                .param("userId", userId)
                .param("targetBalance", targetBalance)
                .param("version", version)
                .update();

        if (updated == 0) {
            throw new OptimisticLockingFailureException("Account reconciliation failed due to concurrent modification");
        }
        return updated;
    }

    public int updateBalance(Long userId, Long accountId, BigDecimal currentBalance, Long version) {
        int updated = jdbcClient.sql(AccountQueries.UPDATE_BALANCE)
                .param("accountId", accountId)
                .param("userId", userId)
                .param("currentBalance", currentBalance)
                .param("version", version)
                .update();
                
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Account balance update failed due to concurrent modification");
        }
        return updated;
    }
}
