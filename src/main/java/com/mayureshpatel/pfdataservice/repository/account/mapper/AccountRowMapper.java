package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AccountRowMapper extends JdbcMapperUtils implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Account.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("name"))
                .typeCode(rs.getString("type"))
                .currentBalance(getBigDecimal(rs, "current_balance"))
                .currencyCode(rs.getString("currency_code"))
                .version(rs.getLong("version"))
                .bankCode(rs.getString("bank_name"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
