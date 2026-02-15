package com.mayureshpatel.pfdataservice.repository.account.model;

import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountTypeRowMapper;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountTypeRepository implements JdbcRepository<AccountTypeLookup, String> {

    private final JdbcClient jdbcClient;
    private final AccountTypeRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    public List<AccountTypeLookup> findByIsActiveTrueOrderBySortOrder() {
        String query = sqlLoader.load("sql/account-type/findBy.sql");
        return this.jdbcClient.sql(query)
                .query(rowMapper)
                .list();
    }
}
