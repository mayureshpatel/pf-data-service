package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AccountSnapshotRowMapper extends JdbcMapperUtils implements RowMapper<AccountSnapshot> {

    @Override
    public AccountSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long accountId = rs.getLong("account_id");
        return AccountSnapshot.builder()
                .id(rs.getLong("id"))
                .accountId(accountId)
                .account(Account.builder().id(accountId).build())
                .snapshotDate(rs.getDate("snapshot_date").toLocalDate())
                .balance(rs.getBigDecimal("balance"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
