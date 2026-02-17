package com.mayureshpatel.pfdataservice.repository.file_import_history.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FileImportHistoryQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from file_import_history
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_ACCOUNT_ID = """
            select *
            from file_import_history
            where account_id = :accountId
            """;

    // language=SQL
    public static final String FIND_BY_FILE_HASH = """
            select *
            from file_import_history
            where file_hash = :fileHash
            """;

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_FILE_HASH = """
            select *
            from file_import_history
            where account_id = :accountId
                and file_hash = :fileHash
            """;

    // language=SQL
    public static final String INSERT = """
            insert into file_import_history (id, account_id, file_hash, file_name, transaction_count, imported_at)
            values (:id, :accountId, :fileHash, :fileName, :transactionCount, current_timestamp)
            """;

    // language=SQL
    public static final String DELETE = """
            delete from file_import_history where id = :id
            """;
}
