package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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

    public List<Account> findByUserId(Long userId) {
        return jdbcClient.sql(AccountQueries.FIND_ALL_BY_USER_ID)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Account insert(Account account) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(AccountQueries.INSERT)
                .param("name", account.getName())
                .param("type", account.getType())
                .param("currentBalance", account.getCurrentBalance())
                .param("currencyCode", account.getCurrency().getCode())
                .param("bankName", account.getBankName() != null ? account.getBankName().name() : null)
                .param("userId", account.getUser().getId())
                .param("createdBy", account.getAudit().getCreatedBy() != null ? account.getAudit().getCreatedBy().getId() : null)
                .param("updatedBy", account.getAudit().getUpdatedBy() != null ? account.getAudit().getUpdatedBy().getId() : null)
                .update(keyHolder);

        account.setId(keyHolder.getKeyAs(Long.class));
        account.setVersion(1L);
        return account;
    }

    @Override
    public Account update(Account account) {
        int updated = jdbcClient.sql(AccountQueries.UPDATE)
                .param("name", account.getName())
                .param("type", account.getType())
                .param("currentBalance", account.getCurrentBalance())
                .param("currencyCode", account.getCurrency().getCode())
                .param("bankName", account.getBankName() != null ? account.getBankName().name() : null)
                .param("updatedBy", account.getAudit().getUpdatedBy() != null ? account.getAudit().getUpdatedBy().getId() : null)
                .param("id", account.getId())
                .param("version", account.getVersion())
                .update();

        if (updated == 0) {
            throw new RuntimeException("Update failed for Account ID: " + account.getId() + ". Version mismatch or already deleted.");
        }

        account.setVersion(account.getVersion() + 1);
        return account;
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            return insert(account);
        } else {
            return update(account);
        }
    }

    @Override
    public void delete(Account account) {
        if (account.getId() != null) {
            Long deletedBy = account.getAudit() != null && account.getAudit().getUpdatedBy() != null
                    ? account.getAudit().getUpdatedBy().getId()
                    : null;
            deleteById(account.getId(), deletedBy);
        }
    }

    @Override
    public void deleteById(Long id, Long deletedBy) {
        jdbcClient.sql(AccountQueries.DELETE_BY_ID)
                .param("id", id)
                .param("deletedBy", deletedBy)
                .update();
    }

    @Override
    public long count() {
        return jdbcClient.sql(AccountQueries.COUNT_ACTIVE)
                .query(Long.class)
                .single();
    }
}
