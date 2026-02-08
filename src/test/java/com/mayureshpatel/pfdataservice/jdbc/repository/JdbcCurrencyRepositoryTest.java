package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcCurrencyRepositoryTest extends JdbcTestBase {

    @Autowired
    private JdbcCurrencyRepository repository;

    @BeforeEach
    void setUp() {
        // Clean up
        jdbcClient.sql("DELETE FROM currencies").update();
    }

    @Test
    void shouldSaveAndFindById() {
        // Given
        Currency currency = new Currency();
        currency.setCode("USD");
        currency.setName("US Dollar");
        currency.setSymbol("$");
        currency.setIsActive(true);

        // When
        repository.save(currency);
        Optional<Currency> found = repository.findById("USD");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("US Dollar");
        assertThat(found.get().getSymbol()).isEqualTo("$");
    }

    @Test
    void shouldFindAllActiveCurrencies() {
        // Given
        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setIsActive(true);
        repository.save(usd);

        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setIsActive(false);
        repository.save(eur);

        // When
        List<Currency> active = repository.findByIsActive(true);

        // Then
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getCode()).isEqualTo("USD");
    }

    @Test
    void shouldUpdateExistingCurrency() {
        // Given
        Currency currency = new Currency();
        currency.setCode("USD");
        currency.setName("US Dollar");
        repository.save(currency);

        // When
        currency.setName("United States Dollar");
        repository.save(currency);

        // Then
        Optional<Currency> updated = repository.findById("USD");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("United States Dollar");
    }
}
