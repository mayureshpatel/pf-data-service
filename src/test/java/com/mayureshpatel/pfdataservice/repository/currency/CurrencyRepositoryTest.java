package com.mayureshpatel.pfdataservice.repository.currency;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(CurrencyRepository.class)
@DisplayName("CurrencyRepository Integration Tests (PostgreSQL)")
class CurrencyRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CurrencyRepository repository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {
        @Test
        @DisplayName("should save and find currency")
        void shouldSaveAndFind() {
            // Arrange
            Currency currency = Currency.builder()
                    .code("GBP")
                    .name("British Pound")
                    .symbol("£")
                    .active(true)
                    .build();

            // Act
            int rows = repository.save(currency);
            Optional<Currency> result = repository.findById("GBP");

            // Assert
            assertEquals(1, rows);
            assertTrue(result.isPresent());
            assertEquals("British Pound", result.get().getName());
        }

        @Test
        @DisplayName("should find all and active currencies")
        void shouldFindAllAndActive() {
            // Arrange
            repository.save(Currency.builder().code("EUR").name("Euro").symbol("€").active(true).build());
            repository.save(Currency.builder().code("JPY").name("Euro").symbol("¥").active(false).build()); // Inactive

            // Act
            List<Currency> all = repository.findAll();
            List<Currency> active = repository.findByIsActive();

            // Assert
            assertTrue(all.size() >= 2);
            assertTrue(active.size() >= 1);
            assertTrue(active.stream().allMatch(Currency::isActive));
        }

        @Test
        @DisplayName("should check existence")
        void shouldCheckExists() {
            // Arrange
            repository.save(Currency.builder().code("CAD").name("Canadian Dollar").symbol("$").active(true).build());

            // Act & Assert
            assertTrue(repository.existsById("CAD"));
            assertFalse(repository.existsById("XYZ"));
        }

        @Test
        @DisplayName("should count currencies")
        void shouldCount() {
            // Arrange
            long initial = repository.count();
            repository.save(Currency.builder().code("ZZA").name("Zza Dollar").symbol("$").active(true).build());

            // Act
            long count = repository.count();

            // Assert
            assertEquals(initial + 1, count);
        }

        @Test
        @DisplayName("should delete by ID")
        void shouldDeleteById() {
            // Arrange
            repository.save(Currency.builder().code("CHF").name("Swiss Franc").symbol("Fr").active(true).build());

            // Act
            int rows = repository.deleteById("CHF");

            // Assert
            assertEquals(1, rows);
            assertFalse(repository.existsById("CHF"));
        }
    }
}
