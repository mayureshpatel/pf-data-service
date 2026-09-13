package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyService Unit Tests")
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    private static final String CURRENCY_CODE = "USD";

    @Nested
    @DisplayName("getAllActiveCurrencies")
    class GetAllActiveCurrenciesTests {
        @Test
        @DisplayName("should return all active currencies from repository")
        void shouldReturnActiveCurrencies() {
            // arrange
            Currency currency = Currency.builder().code("USD").name("US Dollar").active(true).build();
            when(currencyRepository.findByIsActive()).thenReturn(List.of(currency));

            // act
            List<Currency> result = currencyService.getAllActiveCurrencies();

            // assert & verify
            assertEquals(1, result.size());
            assertEquals("USD", result.get(0).getCode());
            verify(currencyRepository).findByIsActive();
        }
    }

    @Nested
    @DisplayName("getCurrencyByCode")
    class GetCurrencyByCodeTests {
        @Test
        @DisplayName("should return currency when code exists")
        void shouldReturnCurrency() {
            // arrange
            Currency currency = Currency.builder().code(CURRENCY_CODE).name("US Dollar").build();
            when(currencyRepository.findById(CURRENCY_CODE)).thenReturn(Optional.of(currency));

            // act
            Currency result = currencyService.getCurrencyByCode(CURRENCY_CODE);

            // assert & verify
            assertNotNull(result);
            assertEquals(CURRENCY_CODE, result.getCode());
            verify(currencyRepository).findById(CURRENCY_CODE);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when code does not exist")
        void shouldThrowException() {
            // arrange
            when(currencyRepository.findById(anyString())).thenReturn(Optional.empty());

            // act & assert & verify
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> currencyService.getCurrencyByCode("XYZ"));
            assertTrue(ex.getMessage().contains("XYZ"));
        }
    }
}
