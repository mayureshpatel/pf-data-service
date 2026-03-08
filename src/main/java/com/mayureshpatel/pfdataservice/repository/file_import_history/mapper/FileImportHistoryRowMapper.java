package com.mayureshpatel.pfdataservice.repository.file_import_history.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import com.mayureshpatel.pfdataservice.repository.account.mapper.AccountRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Component
public class FileImportHistoryRowMapper extends JdbcMapperUtils implements RowMapper<FileImportHistory> {

    @Override
    public FileImportHistory mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, "");
    }

    /**
     * Maps a ResultSet row to a FileImportHistory object.
     *
     * @param rs     ResultSet containing the row data
     * @param prefix Column prefix to use for mapping
     * @return FileImportHistory object
     * @throws SQLException if there is an error accessing the ResultSet
     */
    public static FileImportHistory mapRow(ResultSet rs, String prefix) throws SQLException {
        String safePrefix;
        if (prefix == null || prefix.isEmpty()) {
            safePrefix = "";
        } else {
            safePrefix = prefix.endsWith("_") ? prefix : prefix + "_";
        }
        Set<String> availableColumns = getAvailableColumns(rs);

        FileImportHistory.FileImportHistoryBuilder builder = FileImportHistory.builder();
        builder.id(rs.getLong(safePrefix + "id"));

        if (availableColumns.contains(safePrefix + "account_id")) {
            builder.account(AccountRowMapper.mapRow(rs, safePrefix + "account_"));
        }
        if (availableColumns.contains(safePrefix + "file_name")) {
            builder.fileName(rs.getString(safePrefix + "file_name"));
        }
        if (availableColumns.contains(safePrefix + "file_hash")) {
            builder.fileHash(rs.getString(safePrefix + "file_hash"));
        }
        if (availableColumns.contains(safePrefix + "transaction_count")) {
            builder.transactionCount(rs.getInt(safePrefix + "transaction_count"));
        }
        builder.audit(getAuditColumns(rs, safePrefix, availableColumns));

        return builder.build();
    }
}
