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
            // arrange
            Currency currency = Currency.builder()
                    .code("GBP")
                    .name("British Pound")
                    .symbol("£")
                    .active(true)
                    .build();

            // act
            int rows = repository.save(currency);
            Optional<Currency> result = repository.findById("GBP");

            // assert & verify
            assertEquals(1, rows);
            assertTrue(result.isPresent());
            assertEquals("British Pound", result.get().getName());
        }

        @Test
        @DisplayName("should find all and active currencies")
        void shouldFindAllAndActive() {
            // arrange
            repository.save(Currency.builder().code("EUR").name("Euro").symbol("€").active(true).build());
            repository.save(Currency.builder().code("JPY").name("Euro").symbol("¥").active(false).build()); // Inactive

            // act
            List<Currency> all = repository.findAll();
            List<Currency> active = repository.findByIsActive();

            // assert & verify
            assertTrue(all.size() >= 2);
            assertFalse(active.isEmpty());
            assertTrue(active.stream().allMatch(Currency::isActive));
        }

        @Test
        @DisplayName("should check existence")
        void shouldCheckExists() {
            // arrange
            repository.save(Currency.builder().code("CAD").name("Canadian Dollar").symbol("$").active(true).build());

            // act & assert & verify
            assertTrue(repository.existsById("CAD"));
            assertFalse(repository.existsById("XYZ"));
        }

        @Test
        @DisplayName("should count currencies")
        void shouldCount() {
            // arrange
            long initial = repository.count();
            repository.save(Currency.builder().code("ZZA").name("Zza Dollar").symbol("$").active(true).build());

            // act
            long count = repository.count();

            // assert & verify
            assertEquals(initial + 1, count);
        }

        @Test
        @DisplayName("should delete by ID")
        void shouldDeleteById() {
            // arrange
            repository.save(Currency.builder().code("CHF").name("Swiss Franc").symbol("Fr").active(true).build());

            // act
            int rows = repository.deleteById("CHF");

            // assert & verify
            assertEquals(1, rows);
            assertFalse(repository.existsById("CHF"));
        }
    }
}
