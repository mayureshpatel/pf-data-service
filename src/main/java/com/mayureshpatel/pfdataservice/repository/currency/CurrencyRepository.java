package com.mayureshpatel.pfdataservice.repository.currency;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.currency.mapper.CurrencyRowMapper;
import com.mayureshpatel.pfdataservice.repository.currency.query.CurrencyQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** JDBC-backed persistence for {@link Currency} -- a small, shared (not per-user) lookup table. */
@Repository
@RequiredArgsConstructor
public class CurrencyRepository implements JdbcRepository<Currency, String> {

    private static final String PARAM_CODE = "code";

    private final JdbcClient jdbcClient;
    private final CurrencyRowMapper rowMapper;

    @Override
    public Optional<Currency> findById(String code) {
        return jdbcClient.sql(CurrencyQueries.FIND_BY_CODE)
                .param(PARAM_CODE, code)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Currency> findAll() {
        return jdbcClient.sql(CurrencyQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    /** @return every active currency, ordered by code */
    public List<Currency> findByIsActive() {
        return jdbcClient.sql(CurrencyQueries.FIND_BY_IS_ACTIVE)
                .query(rowMapper)
                .list();
    }

    /**
     * Insert-or-update by {@code code} -- see {@code CurrencyQueries.SAVE}'s {@code on conflict}
     * clause. The only write path this repository exposes; there's no separate {@code insert}/
     * {@code update} pair like most other repositories in this codebase.
     *
     * @param currency the currency to create or overwrite
     * @return the number of rows affected (always 1)
     */
    public int save(Currency currency) {
        return jdbcClient.sql(CurrencyQueries.SAVE)
                .param(PARAM_CODE, currency.getCode())
                .param("name", currency.getName())
                .param("symbol", currency.getSymbol())
                .param("isActive", currency.isActive())
                .update();
    }

    @Override
    public int deleteById(String code) {
        return jdbcClient.sql(CurrencyQueries.DELETE)
                .param(PARAM_CODE, code)
                .update();
    }

    /**
     * @param code the currency code to check
     * @return whether a currency with this code exists
     */
    public boolean existsById(String code) {
        Integer count = jdbcClient.sql(CurrencyQueries.EXISTS_BY_CODE)
                .param(PARAM_CODE, code)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public long count() {
        return jdbcClient.sql(CurrencyQueries.COUNT)
                .query(Long.class)
                .single();
    }
}
