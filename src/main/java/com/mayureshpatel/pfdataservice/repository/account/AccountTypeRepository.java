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

/**
 * JDBC-backed persistence for {@link AccountType} -- a small, shared (not per-user) lookup table,
 * unlike most other repositories in this package.
 */
@Repository
@RequiredArgsConstructor
public class AccountTypeRepository implements JdbcRepository<AccountType, String> {

    private final JdbcClient jdbcClient;
    private final AccountTypeRowMapper rowMapper;

    /** @return every active account type, in display (sort order) sequence */
    public List<AccountType> findByIsActiveTrueOrderBySortOrder() {
        return this.jdbcClient.sql(AccountTypeQueries.FIND_ALL_ORDERED)
                .query(rowMapper)
                .list();
    }

    /**
     * @param request the account type to create
     * @return the number of rows inserted (always 1 on success)
     */
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

    /**
     * @param code the account type's code (primary key)
     * @return the number of rows deleted (0 or 1)
     */
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
