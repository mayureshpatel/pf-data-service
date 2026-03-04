package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AccountTypeRowMapper extends JdbcMapperUtils implements RowMapper<AccountType> {

    @Override
    public AccountType mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AccountType.builder()
                .code(rs.getString("code"))
                .label(rs.getString("label"))
                .color(rs.getString("color"))
                .icon(rs.getString("icon"))
                .asset(rs.getBoolean("is_asset"))
                .sortOrder(rs.getInt("sort_order"))
                .active(rs.getBoolean("is_active"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
