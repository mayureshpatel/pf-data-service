package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class AccountTypeRowMapper extends JdbcMapperUtils implements RowMapper<AccountType> {

    @Override
    public AccountType mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to an AccountType object with prefix support.
     *
     * @param rs    the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped AccountType object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static AccountType mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        Set<String> availableColumns = getAvailableColumns(rs);

        AccountType.AccountTypeBuilder builder = AccountType.builder();
        builder.code(rs.getString(safePrefix + "code"));

        if (hasColumn(safePrefix + "label", availableColumns)) {
            builder.label(rs.getString(safePrefix + "label"));
        }
        if (hasColumn(safePrefix + "color", availableColumns)) {
            builder.color(rs.getString(safePrefix + "color"));
        }
        if (hasColumn(safePrefix + "icon", availableColumns)) {
            builder.icon(rs.getString(safePrefix + "icon"));
        }
        if (hasColumn(safePrefix + "is_asset", availableColumns)) {
            builder.asset(rs.getBoolean(safePrefix + "is_asset"));
        }
        if (hasColumn(safePrefix + "sort_order", availableColumns)) {
            builder.sortOrder(rs.getInt(safePrefix + "sort_order"));
        }
        if (hasColumn(safePrefix + "is_active", availableColumns)) {
            builder.active(rs.getBoolean(safePrefix + "is_active"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
