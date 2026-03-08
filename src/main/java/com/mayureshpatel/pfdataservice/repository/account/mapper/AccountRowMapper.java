package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.currency.mapper.CurrencyRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class AccountRowMapper extends JdbcMapperUtils implements RowMapper<Account> {

    @Override
    public Account mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to an Account object with prefix support.
     *
     * @param rs     the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped Account object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static Account mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        Account.AccountBuilder builder = Account.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "user_id", availableColumns)) {
            builder.userId(rs.getLong(safePrefix + "user_id"));
        }
        if (hasColumn(safePrefix + "name", availableColumns)) {
            builder.name(rs.getString(safePrefix + "name"));
        }
        if (hasColumn(safePrefix + "type", availableColumns)) {
            builder.type(AccountTypeRowMapper.mapRow(rs, "account_type"));
        }
        if (hasColumn(safePrefix + "current_balance", availableColumns)) {
            builder.currentBalance(getBigDecimal(rs, safePrefix + "current_balance"));
        }
        if (hasColumn(safePrefix + "currency_code", availableColumns)) {
            builder.currency(CurrencyRowMapper.mapRow(rs, "currency"));
        }
        if (hasColumn(safePrefix + "bank_name", availableColumns)) {
            builder.bankCode(rs.getString(safePrefix + "bank_name"));
        }
        if (hasColumn(safePrefix + "version", availableColumns)) {
            builder.version(rs.getLong(safePrefix + "version"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
