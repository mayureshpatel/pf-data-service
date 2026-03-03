package com.mayureshpatel.pfdataservice.repository.file_import_history.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;

@Component
public class FileImportHistoryRowMapper implements RowMapper<FileImportHistory> {

    @Override
    public FileImportHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        FileImportHistory fileImportHistory = new FileImportHistory();
        fileImportHistory.setId(rs.getLong("id"));
        
        Account account = new Account();
        account.setId(rs.getLong("account_id"));
        fileImportHistory.setAccount(account);
        
        fileImportHistory.setFileName(rs.getString("file_name"));
        fileImportHistory.setFileHash(rs.getString("file_hash"));
        fileImportHistory.setTransactionCount(rs.getInt("transaction_count"));

        CreatedAtAudit audit = new CreatedAtAudit();
        Timestamp importedAt = rs.getTimestamp("imported_at");
        if (importedAt != null) {
            audit.setCreatedAt(importedAt.toInstant().atOffset(ZoneOffset.UTC));
        }
        fileImportHistory.setAudit(audit);
        
        return fileImportHistory;
    }
}
