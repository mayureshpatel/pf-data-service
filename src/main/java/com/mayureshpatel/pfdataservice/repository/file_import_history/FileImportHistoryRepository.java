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

import java.util.Optional;

/**
 * JDBC-backed persistence for {@link FileImportHistory}. Has two insert paths --
 * {@link #save(FileImportHistory)} (the one actually used, taking a fully-populated domain
 * object with a real post-parse transaction count) and {@link #insert(FileImportCreateRequest)}
 * (currently unused, hardcodes {@code transactionCount} to {@code 0}; see that method's own doc).
 */
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

    /**
     * Backs duplicate-import detection -- a hit here means this exact file was already imported
     * for this account.
     *
     * @param accountId the account the file was imported into
     * @param fileHash  the file's content hash
     * @return the matching import history entry, if this file was already imported for this account
     */
    public Optional<FileImportHistory> findByAccountIdAndFileHash(Long accountId, String fileHash) {
        return jdbcClient.sql(FileImportHistoryQueries.FIND_BY_ACCOUNT_ID_AND_FILE_HASH)
                .param("accountId", accountId)
                .param("fileHash", fileHash)
                .query(rowMapper)
                .optional();
    }

    /**
     * Currently unused (no caller in this codebase) -- always records {@code transactionCount} as
     * {@code 0}, unlike {@link #save(FileImportHistory)}, the method the real CSV-import flow
     * actually calls once the real count is known. See {@link FileImportCreateRequest}'s own doc
     * for more.
     *
     * @param request the import details to record
     * @return the generated history entry id
     */
    public int insert(FileImportCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long accountId = Long.parseLong(request.getAccountId());
        jdbcClient.sql(FileImportHistoryQueries.INSERT)
                .param("accountId", accountId)
                .param("fileHash", request.getFileHash())
                .param("fileName", request.getFileName())
                .param("transactionCount", 0)
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * The real insert path used by the CSV-import flow, taking a fully-populated domain object
     * (including the actual post-parse {@code transactionCount}) rather than the raw create-request
     * shape {@link #insert(FileImportCreateRequest)} accepts.
     *
     * @param history the import history entry to record
     * @return the generated history entry id
     */
    public int save(FileImportHistory history) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(FileImportHistoryQueries.INSERT)
                .param("accountId", history.getAccount().getId())
                .param("fileHash", history.getFileHash())
                .param("fileName", history.getFileName())
                .param("transactionCount", history.getTransactionCount())
                .update(keyHolder);

        return keyHolder.getKey().intValue();
    }
}
