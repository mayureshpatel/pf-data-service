package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.jdbc.JdbcRepository;
import com.mayureshpatel.pfdataservice.jdbc.mapper.CurrencyRowMapper;
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

    @Override
    public Optional<Currency> findById(String code) {
        return jdbcClient.sql("""
                        SELECT code, name, symbol, is_active, created_at
                        FROM currencies
                        WHERE code = :code
                        """)
                .param("code", code)
                .query(rowMapper)
                .optional();
    }

    @Override
    public List<Currency> findAll() {
        return jdbcClient.sql("""
                        SELECT code, name, symbol, is_active, created_at
                        FROM currencies
                        ORDER BY code
                        """)
                .query(rowMapper)
                .list();
    }

    public List<Currency> findByIsActive(boolean isActive) {
        return jdbcClient.sql("""
                        SELECT code, name, symbol, is_active, created_at
                        FROM currencies
                        WHERE is_active = :isActive
                        ORDER BY code
                        """)
                .param("isActive", isActive)
                .query(rowMapper)
                .list();
    }

    @Override
    public Currency save(Currency currency) {
        jdbcClient.sql("""
                        INSERT INTO currencies (code, name, symbol, is_active)
                        VALUES (:code, :name, :symbol, :isActive)
                        ON CONFLICT (code) DO UPDATE SET
                            name = EXCLUDED.name,
                            symbol = EXCLUDED.symbol,
                            is_active = EXCLUDED.is_active
                        """)
                .param("code", currency.getCode())
                .param("name", currency.getName())
                .param("symbol", currency.getSymbol())
                .param("isActive", currency.getIsActive())
                .update();

        return currency;
    }

    @Override
    public void deleteById(String code) {
        jdbcClient.sql("DELETE FROM currencies WHERE code = :code")
                .param("code", code)
                .update();
    }

    @Override
    public boolean existsById(String code) {
        Integer count = jdbcClient.sql("""
                        SELECT COUNT(*) FROM currencies WHERE code = :code
                        """)
                .param("code", code)
                .query(Integer.class)
                .single();

        return count > 0;
    }

    @Override
    public long count() {
        return jdbcClient.sql("SELECT COUNT(*) FROM currencies")
                .query(Long.class)
                .single();
    }
}
