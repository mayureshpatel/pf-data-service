package com.mayureshpatel.pfdataservice.repository.account.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AccountTypeQueries {

    // language=SQL
    public static final String FIND_ALL_ORDERED = """
            select *
            from account_types
            where is_active = true
            order by sort_order
            """;

    // language=SQL
    public static final String INSERT = """
            insert into account_types (
                                       code,
                                       label,
                                       icon,
                                       color,
                                       is_asset,
                                       sort_order,
                                       is_active,
                                       created_at,
                                       updated_at)
            values (
                    :code,
                    :label,
                    :icon,
                    :color,
                    :isAsset,
                    :sortOrder,
                    :isActive,
                    current_timestamp,
                    current_timestamp
                    )
            """;

    // language=SQL
    public static final String DELETE = """
            delete from account_types
            where code = :code
            """;
}
