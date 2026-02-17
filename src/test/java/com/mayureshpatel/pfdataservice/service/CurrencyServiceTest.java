package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency currency;

    @BeforeEach
    void setUp() {
        currency = new Currency();
        currency.setCode("USD");
        currency.setName("US Dollar");
        currency.setSymbol("$");
        currency.setActive(true);
    }

    @Test
    void getAllActiveCurrencies_ShouldReturnListOfActiveCurrencies() {
        // Given
        when(currencyRepository.findByIsActive(true)).thenReturn(List.of(currency));

        // When
        List<Currency> currencies = currencyService.getAllActiveCurrencies();

        // Then
        assertThat(currencies).hasSize(1).contains(currency);
        verify(currencyRepository).findByIsActive(true);
    }

    @Test
    void getCurrencyByCode_ShouldReturnCurrencyIfExists() {
        // Given
        when(currencyRepository.findById("USD")).thenReturn(Optional.of(currency));

        // When
        Currency foundCurrency = currencyService.getCurrencyByCode("USD");

        // Then
        assertThat(foundCurrency).isEqualTo(currency);
        verify(currencyRepository).findById("USD");
    }

    @Test
    void getCurrencyByCode_ShouldThrowExceptionIfNotFound() {
        // Given
        when(currencyRepository.findById("EUR")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> currencyService.getCurrencyByCode("EUR"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Currency with code EUR not found");
        verify(currencyRepository).findById("EUR");
    }
}
