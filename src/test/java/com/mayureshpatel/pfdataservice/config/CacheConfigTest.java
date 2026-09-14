package com.mayureshpatel.pfdataservice.config;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.service.CurrencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests proving the Caffeine-backed cache manager actually intercepts calls
 * (PF-321) -- a plain Mockito unit test of {@link CurrencyService} can't exercise this at all,
 * since {@code @Cacheable}'s proxying only exists within a real Spring context. Loads only
 * {@link CacheConfig} and {@link CurrencyService}, not the full application, so this needs no
 * database/Testcontainers.
 */
@SpringBootTest(classes = {CacheConfig.class, CurrencyService.class})
@DisplayName("CacheConfig Integration Tests")
class CacheConfigTest {

    @Autowired
    private CurrencyService currencyService;

    @MockitoBean
    private CurrencyRepository currencyRepository;

    @Nested
    @DisplayName("currencies cache")
    class CurrenciesCacheTests {
        @Test
        @DisplayName("should serve repeated getAllActiveCurrencies calls from the cache, not the repository")
        void shouldServeRepeatedCallsFromCache() {
            // arrange
            Currency usd = Currency.builder().code("USD").name("US Dollar").active(true).build();
            when(currencyRepository.findByIsActive()).thenReturn(List.of(usd));

            // act
            List<Currency> first = currencyService.getAllActiveCurrencies();
            List<Currency> second = currencyService.getAllActiveCurrencies();
            List<Currency> third = currencyService.getAllActiveCurrencies();

            // assert & verify
            assertEquals(first, second);
            assertEquals(first, third);
            verify(currencyRepository, times(1)).findByIsActive();
        }
    }
}
