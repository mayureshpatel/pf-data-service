package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JdbcCurrencyRepository using Testcontainers.
 * Each test run gets a fresh PostgreSQL database with migrations applied.
 */
@SpringBootTest
@Testcontainers
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
    private CurrencyRepository repository;

    @Test
    void shouldSaveAndFindById() {
        // Given
        Currency currency = new Currency();
        currency.setCode("ZZ5");
        currency.setName("Test Dollar");
        currency.setSymbol("@");
        currency.setIsActive(true);

        // When
        repository.save(currency);
        Optional<Currency> found = repository.findById("ZZ5");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Dollar");
        assertThat(found.get().getSymbol()).isEqualTo("@");
    }

    @Test
    void shouldFindAllActiveCurrencies() {
        // Given
        Currency active1 = new Currency();
        active1.setCode("ZZ1");
        active1.setName("Test Active 1");
        active1.setSymbol("@");
        active1.setIsActive(true);
        repository.save(active1);

        Currency inactive = new Currency();
        inactive.setCode("ZZ2");
        inactive.setName("Test Inactive");
        inactive.setSymbol("#");
        inactive.setIsActive(false);
        repository.save(inactive);

        // When
        List<Currency> activeCurrencies = repository.findByIsActive(true);

        // Then
        assertThat(activeCurrencies)
                .isNotEmpty()
                .anyMatch(c -> c.getCode().equals("ZZ1"))
                .noneMatch(c -> c.getCode().equals("ZZ2"));
    }

    @Test
    void shouldUpdateExistingCurrency() {
        // Given
        Currency currency = new Currency();
        currency.setCode("ZZ6");
        currency.setName("Test Dollar");
        currency.setSymbol("@");
        currency.setIsActive(true);
        repository.save(currency);

        // When - update the name
        currency.setName("Updated Test Dollar");
        repository.save(currency);

        // Then
        Optional<Currency> updated = repository.findById("ZZ6");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Updated Test Dollar");
        assertThat(updated.get().getSymbol()).isEqualTo("@");
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
        long initialCount = repository.count();

        Currency c1 = new Currency();
        c1.setCode("ZZ3");
        c1.setName("Test Currency 1");
        c1.setSymbol("@");
        c1.setIsActive(true);
        repository.save(c1);

        Currency c2 = new Currency();
        c2.setCode("ZZ4");
        c2.setName("Test Currency 2");
        c2.setSymbol("#");
        c2.setIsActive(true);
        repository.save(c2);

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(initialCount + 2);
    }
}
