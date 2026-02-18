package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class RecurringTransactionRowMapper implements RowMapper<RecurringTransaction> {

    @Override
    public RecurringTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setId(rs.getLong("id"));
        recurringTransaction.getUser().setId(rs.getLong("user_id"));
        recurringTransaction.getVendor().setName(rs.getString("merchant_name"));
        recurringTransaction.setAmount(rs.getBigDecimal("amount"));
        recurringTransaction.setFrequency(Frequency.valueOf(rs.getString("frequency")));
        recurringTransaction.setLastDate(JdbcMapperUtils.getOffsetDateTime(rs, "last_date"));
        recurringTransaction.setNextDate(JdbcMapperUtils.getOffsetDateTime(rs, "next_date"));
        recurringTransaction.setActive(rs.getBoolean("active"));

        recurringTransaction.getAudit().setCreatedAt(rs.getTimestamp("created_at").toInstant().atOffset(java.time.ZoneOffset.UTC));
        recurringTransaction.getAudit().setUpdatedAt(rs.getTimestamp("updated_at").toInstant().atOffset(java.time.ZoneOffset.UTC));
        recurringTransaction.getAudit().setDeletedAt(rs.getTimestamp("deleted_at").toInstant().atOffset(java.time.ZoneOffset.UTC));

        return recurringTransaction;
    }
}
