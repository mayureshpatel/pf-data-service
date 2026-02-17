package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountTypeLookup;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountTypeRowMapper;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountTypeQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountTypeRepository implements JdbcRepository<AccountTypeLookup, String> {

    private final JdbcClient jdbcClient;
    private final AccountTypeRowMapper rowMapper;

    public List<AccountTypeLookup> findByIsActiveTrueOrderBySortOrder() {
        return this.jdbcClient.sql(AccountTypeQueries.FIND_ALL_ORDERED)
                .query(rowMapper)
                .list();
    }

    @Override
    public AccountTypeLookup save(AccountTypeLookup entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    @Override
    public void delete(AccountTypeLookup entity) {
        if (entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
}
