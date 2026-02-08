package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.model.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JdbcCurrencyRepository using Testcontainers.
 * Each test run gets a fresh PostgreSQL database with migrations applied.
 */
@SpringBootTest
@TestContainer
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class JdbcCurrencyRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcCurrencyRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

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
        usd.setName("US Dollar");
        usd.setSymbol("$");
        usd.setIsActive(true);
        repository.save(usd);

        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");
        eur.setSymbol("€");
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
        currency.setSymbol("$");
        currency.setIsActive(true);
        repository.save(currency);

        // When - update the name
        currency.setName("United States Dollar");
        repository.save(currency);

        // Then
        Optional<Currency> updated = repository.findById("USD");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("United States Dollar");
        assertThat(updated.get().getSymbol()).isEqualTo("$");
    }

    @Test
    void shouldReturnEmptyWhenCurrencyNotFound() {
        // When
        Optional<Currency> found = repository.findById("XXX");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountAllCurrencies() {
        // Given
        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("US Dollar");
        usd.setSymbol("$");
        usd.setIsActive(true);
        repository.save(usd);

        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");
        eur.setSymbol("€");
        eur.setIsActive(true);
        repository.save(eur);

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}
