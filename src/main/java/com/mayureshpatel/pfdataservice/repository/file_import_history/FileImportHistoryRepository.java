package com.mayureshpatel.pfdataservice.repository.file_import_history;

import com.mayureshpatel.pfdataservice.domain.transaction.FileImportHistory;
import com.mayureshpatel.pfdataservice.dto.transaction.fileimport.FileImportCreateRequest;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.file_import_history.mapper.FileImportHistoryRowMapper;
import com.mayureshpatel.pfdataservice.repository.file_import_history.query.FileImportHistoryQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

    public Optional<FileImportHistory> findByAccountIdAndFileHash(Long accountId, String fileHash) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_BY_ACCOUNT_ID_AND_FILE_HASH)
                .param("accountId", accountId)
                .param("fileHash", fileHash)
                .query(rowMapper)
                .optional();
    }

    public Optional<FileImportHistory> findByFileHash(String fileHash) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_BY_FILE_HASH)
                .param("fileHash", fileHash)
                .query(rowMapper)
                .optional();
    }

    public int insert(FileImportCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long accountId = Long.parseLong(request.getAccountId());
        return jdbcClient.sql(FileImportHistoryQueries.INSERT)
                .param("accountId", accountId)
                .param("fileHash", request.getFileHash())
                .param("fileName", request.getFileName())
                .param("transactionCount", 0)
                .update(keyHolder);
    }

    @Override
    public int save(FileImportHistory history) {
        return jdbcClient.sql(FileImportHistoryQueries.INSERT)
                .param("accountId", history.getAccountId())
                .param("fileHash", history.getFileHash())
                .param("fileName", history.getFileName())
                .param("transactionCount", history.getTransactionCount())
                .update();
    }

    public int deleteById(Long id) {
        return jdbcClient.sql(FileImportHistoryQueries.DELETE)
                .param("id", id)
                .update();
    }
}
