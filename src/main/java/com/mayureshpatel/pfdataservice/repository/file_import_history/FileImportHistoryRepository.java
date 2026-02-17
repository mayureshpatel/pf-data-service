package com.mayureshpatel.pfdataservice.repository.file_import_history;

import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.mapper.FileImportHistoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.file_import_history.query.FileImportHistoryQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileImportHistoryRepository implements JdbcRepository<FileImportHistory, Long> {

    private final JdbcClient jdbcClient;
    private final FileImportHistoryRowMapper rowMapper;

    @Override
    public Optional<FileImportHistory> findById(Long id) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_BY_ID)
                .param("id", id)
                .query(rowMapper)
                .optional();
    }

    public List<FileImportHistory> findAllByAccountId(Long id) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_ALL_BY_ACCOUNT_ID)
                .param("accountId", id)
                .query(rowMapper)
                .list();
    }

    public Optional<FileImportHistory> findByFileHash(String fileHash) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_BY_FILE_HASH)
                .param("fileHash", fileHash)
                .query(rowMapper)
                .optional();
    }

    public FileImportHistory insert(FileImportHistory fileImportHistory) {
        jdbcClient.sql(FileImportHistoryQueries.INSERT)
                .param("accountId", fileImportHistory.getAccount().getId())
                .param("fileHash", fileImportHistory.getFileHash())
                .param("fileName", fileImportHistory.getFileName())
                .param("transactionCount", fileImportHistory.getTransactionCount())
                .update();

        return fileImportHistory;
    }

    public void deleteById(Long id) {
        jdbcClient.sql(FileImportHistoryQueries.DELETE)
                .param("id", id)
                .update();
    }

    @Override
    public FileImportHistory save(FileImportHistory entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    @Override
    public void delete(FileImportHistory entity) {
        if (entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
}
