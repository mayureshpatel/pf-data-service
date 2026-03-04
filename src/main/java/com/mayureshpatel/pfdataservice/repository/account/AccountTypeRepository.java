package com.mayureshpatel.pfdataservice.repository.account;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountTypeRowMapper;
import com.mayureshpatel.pfdataservice.repository.account.query.AccountTypeQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountTypeRepository implements JdbcRepository<AccountType, String> {

    private final JdbcClient jdbcClient;
    private final AccountTypeRowMapper rowMapper;

    public List<AccountType> findByIsActiveTrueOrderBySortOrder() {
        return this.jdbcClient.sql(AccountTypeQueries.FIND_ALL_ORDERED)
                .query(rowMapper)
                .list();
    }

    public int insert(AccountTypeCreateRequest request) {
        return jdbcClient.sql(AccountTypeQueries.INSERT)
                .param("code", request.getCode())
                .param("label", request.getLabel())
                .param("icon", request.getIcon())
                .param("color", request.getColor())
                .param("isAsset", request.isAsset())
                .param("sortOrder", request.getSortOrder())
                .param("isActive", request.isActive())
                .update();
    }

    public int deleteByCode(String code) {
        return jdbcClient.sql(AccountTypeQueries.DELETE)
                .param("code", code)
                .update();
    }

    @Override
    public int delete(AccountType entity) {
        if (entity.getCode() != null) {
            return deleteByCode(entity.getCode());
        }
        return 0;
    }
}
