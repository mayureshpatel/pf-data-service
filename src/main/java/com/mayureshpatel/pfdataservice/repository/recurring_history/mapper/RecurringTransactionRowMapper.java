package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;

@Component
public class RecurringTransactionRowMapper implements RowMapper<RecurringTransaction> {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");

    @Override
    public RecurringTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setId(rs.getLong("id"));

        User user = new User();
        user.setId(rs.getLong("user_id"));
        recurringTransaction.setUser(user);

        Merchant merchant = new Merchant();
        merchant.setName(rs.getString("merchant_name"));
        recurringTransaction.setMerchant(merchant);

        recurringTransaction.setAmount(rs.getBigDecimal("amount"));
        recurringTransaction.setFrequency(Frequency.valueOf(rs.getString("frequency")));
        recurringTransaction.setLastDate(JdbcMapperUtils.getOffsetDateTime(rs, "last_date"));
        recurringTransaction.setNextDate(JdbcMapperUtils.getOffsetDateTime(rs, "next_date"));
        recurringTransaction.setActive(rs.getBoolean("active"));

        TableAudit audit = new TableAudit();
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            audit.setCreatedAt(createdAt.toInstant().atZone(EASTERN).toOffsetDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            audit.setUpdatedAt(updatedAt.toInstant().atZone(EASTERN).toOffsetDateTime());
        }

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            audit.setDeletedAt(deletedAt.toInstant().atZone(EASTERN).toOffsetDateTime());
        }
        recurringTransaction.setAudit(audit);

        return recurringTransaction;
    }
}
