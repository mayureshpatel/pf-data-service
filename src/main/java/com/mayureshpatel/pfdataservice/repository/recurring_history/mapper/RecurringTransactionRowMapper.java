package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

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
                .accountId(rs.getLong("account_id"))
                .merchantId(rs.getLong("merchant_id"))
                .frequency(rs.getString("frequency"))
                .lastDate(rs.getDate("last_date").toLocalDate())
                .nextDate(rs.getDate("next_date").toLocalDate())
                .amount(rs.getBigDecimal("amount"))
                .active(rs.getBoolean("active"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
