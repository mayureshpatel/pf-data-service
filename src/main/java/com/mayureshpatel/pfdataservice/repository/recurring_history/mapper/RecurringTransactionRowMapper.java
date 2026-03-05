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
        Long accountId = getLongOrNull(rs, "account_id");
        Long merchantId = getLongOrNull(rs, "merchant_id");
        java.sql.Date lastDateSql = rs.getDate("last_date");
        java.sql.Date nextDateSql = rs.getDate("next_date");

        return RecurringTransaction.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .account(accountId != null ?
                        Account.builder().id(accountId)
                                .userId(rs.getLong("user_id"))
                                .build() : null)
                .merchant(merchantId != null ?
                        Merchant.builder().id(merchantId)
                                .userId(rs.getLong("user_id"))
                                .build() : null)
                .frequency(rs.getString("frequency"))
                .lastDate(lastDateSql != null ? lastDateSql.toLocalDate() : null)
                .nextDate(nextDateSql != null ? nextDateSql.toLocalDate() : null)
                .amount(rs.getBigDecimal("amount"))
                .active(rs.getBoolean("active"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
