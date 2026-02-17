package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
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
        recurringTransaction.setLastDate(rs.getDate("last_date").toLocalDate());
        recurringTransaction.setNextDate(rs.getDate("next_date").toLocalDate());
        recurringTransaction.setActive(rs.getBoolean("active"));

        recurringTransaction.getAudit().setCreatedAt(rs.getTimestamp("created_at").toInstant().atOffset(java.time.ZoneOffset.UTC));
        recurringTransaction.getAudit().setUpdatedAt(rs.getTimestamp("updated_at").toInstant().atOffset(java.time.ZoneOffset.UTC));
        recurringTransaction.getAudit().setDeletedAt(rs.getTimestamp("deleted_at").toInstant().atOffset(java.time.ZoneOffset.UTC));

        return recurringTransaction;
    }
}
