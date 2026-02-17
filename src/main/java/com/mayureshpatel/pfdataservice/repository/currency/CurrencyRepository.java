package com.mayureshpatel.pfdataservice.repository.currency;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.JdbcRepository;
import com.mayureshpatel.pfdataservice.repository.SqlLoader;
import com.mayureshpatel.pfdataservice.repository.currency.mapper.CurrencyRowMapper;
import com.mayureshpatel.pfdataservice.repository.currency.query.CurrencyQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CurrencyRepository implements JdbcRepository<Currency, String> {

    private final JdbcClient jdbcClient;
    private final CurrencyRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Currency> findById(String code) {
        return jdbcClient.sql(CurrencyQueries.FIND_BY_CODE)
                .param("code", code)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Currency> findAll() {
        return jdbcClient.sql(CurrencyQueries.FIND_ALL)
                .query(rowMapper)
                .list();
    }

    public List<Currency> findByIsActive() {
        return jdbcClient.sql(CurrencyQueries.FIND_BY_IS_ACTIVE)
                .query(rowMapper)
                .list();
    }

    public Currency save(Currency currency) {
        jdbcClient.sql(CurrencyQueries.SAVE)
                .param("code", currency.getCode())
                .param("name", currency.getName())
                .param("symbol", currency.getSymbol())
                .param("isActive", currency.getIsActive())
                .update();

        return currency;
    }

    @Override
    public void deleteById(String code) {
        jdbcClient.sql(CurrencyQueries.DELETE)
                .param("code", code)
                .update();
    }

    public boolean existsById(String code) {
        Integer count = jdbcClient.sql(CurrencyQueries.COUNT)
                .param("code", code)
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
