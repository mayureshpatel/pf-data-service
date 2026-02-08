package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.mapper.CurrencyRowMapper;
import com.mayureshpatel.pfdataservice.jdbc.util.SqlLoader;
import com.mayureshpatel.pfdataservice.model.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcCurrencyRepository implements JdbcRepository<Currency, String> {

    private final JdbcClient jdbcClient;
    private final CurrencyRowMapper rowMapper;
    private final SqlLoader sqlLoader;

    @Override
    public Optional<Currency> findById(String code) {
        String sql = sqlLoader.load("sql/currency/findById.sql");
        return jdbcClient.sql(sql)
                .param("code", code)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Currency> findAll() {
        String sql = sqlLoader.load("sql/currency/findAll.sql");
        return jdbcClient.sql(sql)
                .query(rowMapper)
                .list();
    }

    public List<Currency> findByIsActive(boolean isActive) {
        String sql = sqlLoader.load("sql/currency/findByIsActive.sql");
        return jdbcClient.sql(sql)
                .param("isActive", isActive)
                .query(rowMapper)
                .list();
    }

    @Override
    public Currency save(Currency currency) {
        String sql = sqlLoader.load("sql/currency/save.sql");
        jdbcClient.sql(sql)
                .param("code", currency.getCode())
                .param("name", currency.getName())
                .param("symbol", currency.getSymbol())
                .param("isActive", currency.getIsActive())
                .update();

        return currency;
    }

    @Override
    public void deleteById(String code) {
        String sql = sqlLoader.load("sql/currency/deleteById.sql");
        jdbcClient.sql(sql)
                .param("code", code)
                .update();
    }

    @Override
    public boolean existsById(String code) {
        String sql = sqlLoader.load("sql/currency/existsById.sql");
        Integer count = jdbcClient.sql(sql)
                .param("code", code)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public long count() {
        String sql = sqlLoader.load("sql/currency/count.sql");
        return jdbcClient.sql(sql)
                .query(Long.class)
                .single();
    }
}
