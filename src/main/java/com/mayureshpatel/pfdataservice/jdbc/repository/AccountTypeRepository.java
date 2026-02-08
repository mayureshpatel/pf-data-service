package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.mapper.AccountTypeRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.AccountTypeLookup;
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
