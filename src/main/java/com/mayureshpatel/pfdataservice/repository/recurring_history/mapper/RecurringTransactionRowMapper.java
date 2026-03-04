package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class RecurringTransactionRowMapper extends JdbcMapperUtils implements RowMapper<RecurringTransaction> {

    @Override
    public RecurringTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return RecurringTransaction.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .account(
                        Account.builder().id(rs.getLong("account_id"))
                                .userId(rs.getLong("user_id"))
                                .build())
                .merchant(
                        Merchant.builder().id(rs.getLong("merchant_id"))
                                .userId(rs.getLong("user_id"))
                                .build())
                .frequency(rs.getString("frequency"))
                .lastDate(rs.getDate("last_date").toLocalDate())
                .nextDate(rs.getDate("next_date").toLocalDate())
                .amount(rs.getBigDecimal("amount"))
                .active(rs.getBoolean("active"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
