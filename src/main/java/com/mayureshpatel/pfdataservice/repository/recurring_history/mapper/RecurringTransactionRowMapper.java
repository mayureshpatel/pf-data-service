package com.mayureshpatel.pfdataservice.repository.recurring_history.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.RecurringTransaction;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
import com.mayureshpatel.pfdataservice.repository.merchant.mapper.MerchantRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class RecurringTransactionRowMapper extends JdbcMapperUtils implements RowMapper<RecurringTransaction> {

    @Override
    public RecurringTransaction mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a RecurringTransaction object.
     *
     * @param rs     the ResultSet to map
     * @param prefix the prefix to use for column names
     * @return the mapped RecurringTransaction object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public RecurringTransaction mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        RecurringTransaction.RecurringTransactionBuilder builder = RecurringTransaction.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "account_id", availableColumns)) {
            builder.account(AccountRowMapper.mapRow(rs, safePrefix + "account"));
        }
        if (hasColumn(safePrefix + "merchant_id", availableColumns)) {
            builder.merchant(MerchantRowMapper.mapRow(rs, safePrefix + "merchant"));
        }
        if (hasColumn(safePrefix + "frequency", availableColumns)) {
            builder.frequency(rs.getString(safePrefix + "frequency"));
        }
        if (hasColumn(safePrefix + "last_date", availableColumns)) {
            builder.lastDate(getLocalDate(rs, safePrefix + "last_date"));
        }
        if (hasColumn(safePrefix + "next_date", availableColumns)) {
            builder.nextDate(getLocalDate(rs, safePrefix + "next_date"));
        }
        if (hasColumn(safePrefix + "amount", availableColumns)) {
            builder.amount(rs.getBigDecimal(safePrefix + "amount"));
        }
        if (hasColumn(safePrefix + "active", availableColumns)) {
            builder.active(rs.getBoolean(safePrefix + "active"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
