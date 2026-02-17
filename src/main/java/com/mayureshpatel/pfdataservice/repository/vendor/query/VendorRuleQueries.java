package com.mayureshpatel.pfdataservice.repository.vendor.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class VendorRuleQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from vendor_rules
            where id = :id
            """;

    // language=SQL
    public static final String FIND_BY_USER_OR_GLOBAL = """
            select *
            from vendor_rules
            where user_id = :userId
               or user_id is null
            order by priority desc, length(keyword) desc
            """;

    // language=SQL
    public static final String INSERT = """
            insert into vendor_rules
                (keyword, vendor_name, priority, user_id, created_at, updated_at)
            values (
                    :keyword,
                    :vendorName,
                    :priority,
                    :userId,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update vendor_rules
            set keyword = :keyword,
                vendor_name = :vendorName,
                priority = :priority,
                updated_at = CURRENT_TIMESTAMP
            where id = :id
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            delete from vendor_rules
            where id = :id
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*)
            from vendor_rules
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from vendor_rules
            where id = :id
            """;
}
