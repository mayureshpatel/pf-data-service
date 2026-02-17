package com.mayureshpatel.pfdataservice.repository.currency.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CurrencyQueries {

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from currencies
            order by code
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*) from currencies
            where is_active = true
                and code = :code
            """;

    // language=SQl
    public static final String DELETE = """
            delete from currencies where code = :code
            """;

    // language=SQL
    public static final String EXISTS_BY_CODE = """
            select count(*) from currencies where code = :code
            """;

    // language=SQL
    public static final String FIND_BY_CODE = """
            select * from currencies where code = :code
            """;

    // language=SQl
    public static final String FIND_BY_IS_ACTIVE = """
            select * from currencies where is_active = true order by code
            """;

    // language=SQL
    public static final String SAVE = """
            insert into currencies (code, name, symbol, is_active)
            values (:code, :name, :symbol, :isActive)
            on conflict (code) do update set
                name = EXCLUDED.name,
                symbol = EXCLUDED.symbol,
                is_active = EXCLUDED.is_active
            """;
}
