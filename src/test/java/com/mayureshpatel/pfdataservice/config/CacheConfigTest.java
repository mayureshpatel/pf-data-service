package com.mayureshpatel.pfdataservice.config;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeCreateRequest;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import com.mayureshpatel.pfdataservice.repository.account.AccountTypeRepository;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import com.mayureshpatel.pfdataservice.service.AccountTypeService;
import com.mayureshpatel.pfdataservice.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests proving the Caffeine-backed cache manager actually intercepts calls
 * (PF-321, PF-322) -- a plain Mockito unit test of a {@code @Cacheable}/{@code @CacheEvict}
 * service can't exercise either annotation at all, since Spring's caching proxy only exists
 * within a real Spring context. Loads only {@link CacheConfig} plus the two cached services, not
 * the full application, so this needs no database/Testcontainers.
 */
@SpringBootTest(classes = {CacheConfig.class, CurrencyService.class, AccountTypeService.class})
@DisplayName("CacheConfig Integration Tests")
class CacheConfigTest {

    @Autowired
    private CurrencyService currencyService;

    @MockitoBean
    private CurrencyRepository currencyRepository;

    @Autowired
    private AccountTypeService accountTypeService;

    @MockitoBean
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private CacheManager cacheManager;

    /**
     * The cache manager is a real, shared bean across every test method in this class (Spring
     * reuses the application context for identical configuration) -- without an explicit clear,
     * a cache entry populated by one test would still be live for the next, making these tests
     * pass or fail depending on execution order instead of on their own actual behavior.
     */
    @BeforeEach
    void clearCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

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

    @Nested
    @DisplayName("account types cache (PF-322)")
    class AccountTypesCacheTests {
        @Test
        @DisplayName("should serve repeated getAllActiveAccountTypes calls from the cache, not the repository")
        void shouldServeRepeatedCallsFromCache() {
            // arrange
            AccountType checking = AccountType.builder().code("CHECKING").label("Checking").active(true).build();
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(checking));

            // act
            List<AccountTypeDto> first = accountTypeService.getAllActiveAccountTypes();
            List<AccountTypeDto> second = accountTypeService.getAllActiveAccountTypes();

            // assert & verify
            assertEquals(first, second);
            verify(accountTypeRepository, times(1)).findByIsActiveTrueOrderBySortOrder();
        }

        @Test
        @DisplayName("should evict the cache on create, so the next list call reflects the new type immediately")
        void shouldEvictCacheOnCreate() {
            // arrange -- prime the cache with the pre-create state
            AccountType checking = AccountType.builder().code("CHECKING").label("Checking").active(true).build();
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(checking));
            accountTypeService.getAllActiveAccountTypes();

            // act -- create, then re-stub the repository to reflect the new row, matching what a
            // real insert-then-reselect would return
            AccountType savings = AccountType.builder().code("SAVINGS").label("Savings").active(true).build();
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(checking, savings));
            accountTypeService.create(AccountTypeCreateRequest.builder().code("SAVINGS").label("Savings").build());
            List<AccountTypeDto> afterCreate = accountTypeService.getAllActiveAccountTypes();

            // assert & verify -- not served from the stale, pre-create cached entry
            assertEquals(2, afterCreate.size());
            verify(accountTypeRepository, times(2)).findByIsActiveTrueOrderBySortOrder();
        }

        @Test
        @DisplayName("should evict the cache on delete, so the next list call reflects the removal immediately")
        void shouldEvictCacheOnDelete() {
            // arrange -- prime the cache with the pre-delete state
            AccountType checking = AccountType.builder().code("CHECKING").label("Checking").active(true).build();
            AccountType savings = AccountType.builder().code("SAVINGS").label("Savings").active(true).build();
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(checking, savings));
            accountTypeService.getAllActiveAccountTypes();

            // act
            when(accountTypeRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(checking));
            accountTypeService.delete("SAVINGS");
            List<AccountTypeDto> afterDelete = accountTypeService.getAllActiveAccountTypes();

            // assert & verify -- not served from the stale, pre-delete cached entry
            assertEquals(1, afterDelete.size());
            verify(accountTypeRepository, times(2)).findByIsActiveTrueOrderBySortOrder();
        }
    }
}
