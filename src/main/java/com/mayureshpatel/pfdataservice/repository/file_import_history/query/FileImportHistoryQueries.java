package com.mayureshpatel.pfdataservice.repository.file_import_history.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FileImportHistoryQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select
                file_import_history.*,
                accounts.name as account_name,
                accounts.type as account_type,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                accounts.bank_name as account_bank_name
            from file_import_history
                left join accounts on file_import_history.account_id = accounts.id
                left join account_types on account_types.code = accounts.type
                left join currencies on currencies.code = accounts.currency_code
            where file_import_history.id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_ACCOUNT_ID = """
            select
                file_import_history.*,
                accounts.name as account_name,
                accounts.type as account_type,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                accounts.bank_name as account_bank_name
            from file_import_history
                left join accounts on file_import_history.account_id = accounts.id
                left join account_types on account_types.code = accounts.type
                left join currencies on currencies.code = accounts.currency_code
            where accounts.id = :accountId
            """;

    // language=SQL
    public static final String FIND_BY_FILE_HASH = """
            select
                file_import_history.*,
                accounts.name as account_name,
                accounts.type as account_type,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                accounts.bank_name as account_bank_name
            from file_import_history
                left join accounts on file_import_history.account_id = accounts.id
                left join account_types on account_types.code = accounts.type
                left join currencies on currencies.code = accounts.currency_code
            where file_import_history.file_hash = :fileHash
            """;

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_FILE_HASH = """
            select
                file_import_history.*,
                accounts.name as account_name,
                accounts.type as account_type,
                account_types.label as account_type_label,
                account_types.color as account_type_color,
                account_types.icon as account_type_icon,
                account_types.is_asset as account_type_is_asset,
                currencies.code as currency_code,
                currencies.name as currency_name,
                currencies.symbol as currency_symbol,
                accounts.bank_name as account_bank_name
            from file_import_history
                left join accounts on file_import_history.account_id = accounts.id
                left join account_types on account_types.code = accounts.type
                left join currencies on currencies.code = accounts.currency_code
            where accounts.id = :accountId
                and file_import_history.file_hash = :fileHash
            """;

    // language=SQL
    public static final String INSERT = """
            insert into file_import_history (account_id, file_hash, file_name, transaction_count, imported_at)
            values (:accountId, :fileHash, :fileName, :transactionCount, current_timestamp)
            returning id
            """;

    // language=SQL
    public static final String DELETE = """
            delete from file_import_history where id = :id
            """;
}
