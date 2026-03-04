package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.transaction.Frequency;
import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;

@Component
public class RecurringTransactionRowMapper implements RowMapper<RecurringTransaction> {

    @Override
    public RecurringTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setId(rs.getLong("id"));

        User user = new User();
        user.setId(rs.getLong("user_id"));
        recurringTransaction.setUser(user);

        long accountId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            Account account = new Account();
            account.setId(accountId);
            recurringTransaction.setAccount(account);
        }

        long merchantId = rs.getLong("merchant_id");
        if (!rs.wasNull()) {
            Merchant merchant = new Merchant();
            merchant.setId(merchantId);
            recurringTransaction.setMerchant(merchant);
        }

        recurringTransaction.setAmount(rs.getBigDecimal("amount"));
        recurringTransaction.setFrequency(Frequency.valueOf(rs.getString("frequency")));

        Date lastDate = rs.getDate("last_date");
        if (lastDate != null) {
            recurringTransaction.setLastDate(lastDate.toLocalDate());
        }

        Date nextDate = rs.getDate("next_date");
        if (nextDate != null) {
            recurringTransaction.setNextDate(nextDate.toLocalDate());
        }

        recurringTransaction.setActive(rs.getBoolean("active"));

        SoftDeleteAudit audit = new SoftDeleteAudit();
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            audit.setCreatedAt(createdAt.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            audit.setUpdatedAt(updatedAt.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            audit.setDeletedAt(deletedAt.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }
        recurringTransaction.setAudit(audit);

        return recurringTransaction;
    }
}
