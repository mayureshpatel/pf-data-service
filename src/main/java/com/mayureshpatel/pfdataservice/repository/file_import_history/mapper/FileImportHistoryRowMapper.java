package com.mayureshpatel.pfdataservice.repository.file_import_history.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.repository.JdbcMapperUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FileImportHistoryRowMapper extends JdbcMapperUtils implements RowMapper<FileImportHistory> {

    @Override
    public FileImportHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return FileImportHistory.builder()
                .id(rs.getLong("id"))
                .accountId(rs.getLong("account_id"))
                .fileName(rs.getString("file_name"))
                .fileHash(rs.getString("file_hash"))
                .transactionCount(rs.getInt("transaction_count"))
                .audit(getAuditColumns(rs))
                .build();
    }
}
