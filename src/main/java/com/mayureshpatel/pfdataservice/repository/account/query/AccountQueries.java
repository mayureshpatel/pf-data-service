package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AccountQueries {

    // language=SQL
    public static final String COUNT_ACTIVE = """
            select count(*)
            from accounts
            where deleted_at is null
            """;

    // language=SQL
    public static final String COUNT_ALL = """
            select count(*)
            from accounts
            """;

    // language=SQL
    public static final String COUNT_DELETED = """
            select count(*)
            from accounts
            where deleted_at is not null
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from accounts
            where deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_ID = """
            select accounts.*,
                   account_types.code as account_type_code,
                   account_types.label as account_type_label,
                   account_types.icon as account_type_icon,
                   account_types.color as account_type_color,
                   account_types.is_asset as account_type_is_asset,
                   account_types.sort_order as account_type_sort_order,
                   account_types.is_active as account_type_is_active
            from accounts
            left join account_types on accounts.type = account_types.code
            where id = :id
                and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_ACCOUNT_ID_AND_USER_ID = """
            select accounts.*,
                   account_types.code as account_type_code,
                   account_types.label as account_type_label,
                   account_types.icon as account_type_icon,
                   account_types.color as account_type_color,
                   account_types.is_asset as account_type_is_asset,
                   account_types.sort_order as account_type_sort_order,
                   account_types.is_active as account_type_is_active
            from accounts
            left join account_types on accounts.type = account_types.code
            where accounts.id = :accountId
                and accounts.user_id = :userId
                and accounts.deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select accounts.*,
                   account_types.code as account_type_code,
                   account_types.label as account_type_label,
                   account_types.icon as account_type_icon,
                   account_types.color as account_type_color,
                   account_types.is_asset as account_type_is_asset,
                   account_types.sort_order as account_type_sort_order,
                   account_types.is_active as account_type_is_active
            from accounts
            left join account_types on accounts.type = account_types.code
            where user_id = :userId
                and deleted_at is null
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from accounts
            where id = :id
              and deleted_at IS NULL
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            update accounts
            set deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :deletedBy
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String INSERT = """
            insert into accounts
                (
                 name,
                 type,
                 current_balance,
                 currency_code,
                 bank_name,
                 user_id,
                 version,
                 created_at, created_by, updated_at, updated_by
                 )
            values(
                   :name,
                   :type,
                   :currentBalance,
                   :currencyCode,
                   :bankName,
                   :userId,
                   1,
                   CURRENT_TIMESTAMP, :createdBy, CURRENT_TIMESTAMP, :updatedBy
                   )
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
                    update accounts
                    set name = :name,
                        type = :type,
                        currency_code = :currencyCode,
                        bank_name = :bankName,
                        version = version + 1,
                        updated_at = CURRENT_TIMESTAMP, updated_by = :updatedBy
                    where id = :id
                      and version = :version
                      and deleted_at is null
            """;

    // language=SQL
    public static final String RECONCILE = """
            update accounts
            set current_balance = :targetBalance
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP, updated_by = :userId
            where id = :accountId
                and version = :version
                and deleted_at is null
            """;
}
