package com.mayureshpatel.pfdataservice.repository.currency.query;

import lombok.NoArgsConstructor;

/**
 * SQL query constants for {@code CurrencyRepository}. {@link #SAVE} is an upsert (insert-or-update
 * on conflict) rather than separate {@code INSERT}/{@code UPDATE} constants -- currencies are a
 * small, admin-maintained lookup table where "save" is the only operation callers actually need.
 */
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
            """;

    // language=SQL
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

    // language=SQL
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
