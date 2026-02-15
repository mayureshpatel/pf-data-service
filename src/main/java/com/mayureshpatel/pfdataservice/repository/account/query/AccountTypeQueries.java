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
}
