//package com.mayureshpatel.pfdataservice.service;
//
//import com.mayureshpatel.pfdataservice.domain.currency.Currency;
//import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
//import com.mayureshpatel.pfdataservice.repository.currency.CurrencyRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("CurrencyService unit tests")
//class CurrencyServiceTest {
//
//    @Mock
//    private CurrencyRepository currencyRepository;
//
//    @InjectMocks
//    private CurrencyService currencyService;
//
//    private Currency buildCurrency(String code, String name, String symbol, Boolean active) {
//        Currency currency = new Currency();
//        currency.setCode(code);
//        currency.setName(name);
//        currency.setSymbol(symbol);
//        currency.setActive(active);
//        return currency;
//    }
//
//    @Nested
//    @DisplayName("getAllActiveCurrencies")
//    class GetAllActiveCurrenciesTest {
//
//        @Test
//        @DisplayName("should return all active currencies from the repository")
//        void getAllActiveCurrencies_activeCurrenciesExist_returnsAll() {
//            Currency usd = buildCurrency("USD", "US Dollar", "$", true);
//            Currency eur = buildCurrency("EUR", "Euro", "€", true);
//            when(currencyRepository.findByIsActive()).thenReturn(List.of(usd, eur));
//
//            List<Currency> result = currencyService.getAllActiveCurrencies();
//
//            assertThat(result).hasSize(2);
//            assertThat(result).extracting(Currency::getCode).containsExactly("USD", "EUR");
//            assertThat(result).extracting(Currency::getName).containsExactly("US Dollar", "Euro");
//            assertThat(result).extracting(Currency::getSymbol).containsExactly("$", "€");
//            assertThat(result).allMatch(Currency::isActive);
//            verify(currencyRepository).findByIsActive();
//            verifyNoMoreInteractions(currencyRepository);
//        }
//
//        @Test
//        @DisplayName("should return empty list when no active currencies exist")
//        void getAllActiveCurrencies_noActiveCurrencies_returnsEmptyList() {
//            when(currencyRepository.findByIsActive()).thenReturn(List.of());
//
//            List<Currency> result = currencyService.getAllActiveCurrencies();
//
//            assertThat(result).isEmpty();
//            verify(currencyRepository).findByIsActive();
//        }
//
//        @Test
//        @DisplayName("should return exactly one currency when only one active currency exists")
//        void getAllActiveCurrencies_singleActiveCurrency_returnsListOfOne() {
//            Currency gbp = buildCurrency("GBP", "British Pound", "£", true);
//            when(currencyRepository.findByIsActive()).thenReturn(List.of(gbp));
//
//            List<Currency> result = currencyService.getAllActiveCurrencies();
//
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).getCode()).isEqualTo("GBP");
//            assertThat(result.get(0).getName()).isEqualTo("British Pound");
//            assertThat(result.get(0).getSymbol()).isEqualTo("£");
//            assertThat(result.get(0).isActive()).isTrue();
//        }
//
//        @Test
//        @DisplayName("should call findByIsActive on the repository, not findAll")
//        void getAllActiveCurrencies_delegatesToFindByIsActive() {
//            when(currencyRepository.findByIsActive()).thenReturn(List.of());
//
//            currencyService.getAllActiveCurrencies();
//
//            verify(currencyRepository).findByIsActive();
//            verifyNoMoreInteractions(currencyRepository);
//        }
//    }
//
//    @Nested
//    @DisplayName("getCurrencyByCode")
//    class GetCurrencyByCodeTest {
//
//        @Test
//        @DisplayName("should return the matching Currency when the code exists")
//        void getCurrencyByCode_codeExists_returnsCurrency() {
//            Currency usd = buildCurrency("USD", "US Dollar", "$", true);
//            when(currencyRepository.findById("USD")).thenReturn(Optional.of(usd));
//
//            Currency result = currencyService.getCurrencyByCode("USD");
//
//            assertThat(result.getCode()).isEqualTo("USD");
//            assertThat(result.getName()).isEqualTo("US Dollar");
//            assertThat(result.getSymbol()).isEqualTo("$");
//            assertThat(result.isActive()).isTrue();
//            verify(currencyRepository).findById("USD");
//            verifyNoMoreInteractions(currencyRepository);
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when code is not found")
//        void getCurrencyByCode_codeNotFound_throwsResourceNotFoundException() {
//            when(currencyRepository.findById("XYZ")).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> currencyService.getCurrencyByCode("XYZ"))
//                    .isInstanceOf(ResourceNotFoundException.class)
//                    .hasMessageContaining("Currency")
//                    .hasMessageContaining("XYZ");
//        }
//
//        @ParameterizedTest(name = "code=''{0}'' should be passed verbatim to findById")
//        @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CAD"})
//        @DisplayName("should forward the exact code string to the repository")
//        void getCurrencyByCode_variousCodes_passesExactCodeToRepository(String code) {
//            Currency currency = buildCurrency(code, "Some Currency", "S", true);
//            when(currencyRepository.findById(code)).thenReturn(Optional.of(currency));
//
//            Currency result = currencyService.getCurrencyByCode(code);
//
//            assertThat(result.getCode()).isEqualTo(code);
//            verify(currencyRepository).findById(code);
//        }
//
//        @Test
//        @DisplayName("should return an inactive currency when the code matches an inactive record")
//        void getCurrencyByCode_inactiveCurrencyFound_returnsIt() {
//            Currency inactive = buildCurrency("OLD", "Old Currency", "O", false);
//            when(currencyRepository.findById("OLD")).thenReturn(Optional.of(inactive));
//
//            Currency result = currencyService.getCurrencyByCode("OLD");
//
//            assertThat(result.getCode()).isEqualTo("OLD");
//            assertThat(result.isActive()).isFalse();
//        }
//
//        @Test
//        @DisplayName("should return currency with null symbol without throwing")
//        void getCurrencyByCode_currencyWithNullSymbol_returnsWithoutThrowing() {
//            Currency currency = buildCurrency("XXX", "No Symbol Currency", null, true);
//            when(currencyRepository.findById("XXX")).thenReturn(Optional.of(currency));
//
//            Currency result = currencyService.getCurrencyByCode("XXX");
//
//            assertThat(result.getCode()).isEqualTo("XXX");
//            assertThat(result.getSymbol()).isNull();
//        }
//    }
//}
