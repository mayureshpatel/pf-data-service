package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.SoftDeleteSupport;
import com.mayureshpatel.pfdataservice.jdbc.mapper.AccountRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.Account;
import lombok.RequiredArgsConstructor;
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
    private final SqlLoader sqlLoader;

    @Override
    public List<Account> findAll() {
        String query = sqlLoader.load("sql/account/findAll.sql");
        return jdbcClient.sql(query)
                .query(rowMapper)
                .list();
    }

    @Override
    public Optional<Account> findById(Long id) {
        String query = sqlLoader.load("sql/account/findById.sql");
        return jdbcClient.sql(query)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public List<Account> findByUserId(Long userId) {
        String query = sqlLoader.load("sql/account/findByUserId.sql");
        return jdbcClient.sql(query)
                .param("userId", userId)
                .query(rowMapper)
                .list();
    }

    @Override
    public Account insert(Account account) {
        String query = sqlLoader.load("sql/account/insert.sql");
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql(query)
                .param("name", account.getName())
                .param("type", account.getType())
                .param("currentBalance", account.getCurrentBalance())
                .param("currencyCode", account.getCurrencyCode())
                .param("bankName", account.getBankName() != null ? account.getBankName().name() : null)
                .param("userId", account.getUser().getId())
                .param("createdBy", account.getCreatedBy() != null ? account.getCreatedBy().getId() : null)
                .param("updatedBy", account.getUpdatedBy() != null ? account.getUpdatedBy().getId() : null)
                .update(keyHolder);

        account.setId(keyHolder.getKeyAs(Long.class));
        return account;
    }

    @Override
    public Account update(Account account) {
        String query = sqlLoader.load("sql/account/update.sql");

        int updated = jdbcClient.sql(query)
                .param("name", account.getName())
                .param("type", account.getType())
                .param("currentBalance", account.getCurrentBalance())
                .param("currencyCode", account.getCurrencyCode())
                .param("bankName", account.getBankName() != null ? account.getBankName().name() : null)
                .param("updatedBy", account.getUpdatedBy() != null ? account.getUpdatedBy().getId() : null)
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
    public void deleteById(Long id, Long deletedBy) {
        String query = sqlLoader.load("sql/account/deleteById.sql");
        jdbcClient.sql(query)
                .param("id", id)
                .param("deletedBy", deletedBy)
                .update();
    }

    @Override
    public long count() {
        String query = sqlLoader.load("sql/account/count.sql");
        return jdbcClient.sql(query)
                .query(Long.class)
                .single();
    }
}
