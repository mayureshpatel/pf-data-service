package com.mayureshpatel.pfdataservice.repository.account.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountSnapshot;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class AccountSnapshotRowMapper extends JdbcMapperUtils implements RowMapper<AccountSnapshot> {

    @Override
    public AccountSnapshot mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to an AccountSnapshot object with prefix support.
     *
     * @param rs    the ResultSet containing the row data
     * @param prefix the prefix to use for column names
     * @return the mapped AccountSnapshot object
     * @throws SQLException if an error occurs while accessing the ResultSet
     */
    public static AccountSnapshot mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        AccountSnapshot.AccountSnapshotBuilder builder = AccountSnapshot.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (hasColumn(safePrefix + "account_id", availableColumns)) {
            builder.accountId(rs.getLong(safePrefix + "account_id"));
        }
        if (hasColumn(safePrefix + "snapshot_date", availableColumns)) {
            builder.snapshotDate(getLocalDate(rs, safePrefix + "snapshot_date"));
        }
        if (hasColumn(safePrefix + "balance", availableColumns)) {
            builder.balance(rs.getBigDecimal(safePrefix + "balance"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
