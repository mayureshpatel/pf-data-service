package com.mayureshpatel.pfdataservice.repository.merchant.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MerchantQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from merchants
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from merchants
            where user_id = :userId
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAME = """
            select *
            from merchants
            where clean_name = :cleanName
            """;

    // language=SQL
    public static final String FIND_ALL_BY_CLEAN_NAME_LIKE = """
            select *
            from merchants
            where clean_name like :cleanName
            """;
}
