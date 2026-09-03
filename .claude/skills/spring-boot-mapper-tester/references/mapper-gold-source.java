package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.domain.currency.Currency;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Gold Standard examples for Mapper unit testing.
 * Demonstrates testing for full mapping, partial mapping, and null handling using @Nested organization.
 */
@DisplayName("Mapper Gold Standard Tests")
class MapperGoldStandardTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<AccountDtoMapper> constructor = AccountDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        AccountDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // act
            AccountDto result = AccountDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            Account account = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Checking")
                    .type(AccountType.builder().code("CHECKING").label("Checking").build())
                    .currentBalance(new BigDecimal("1500.00"))
                    .currency(Currency.builder().code("USD").name("US Dollar").build())
                    .bankCode("CAPITAL_ONE")
                    .build();

            // act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNotNull(dto);
            assertEquals(account.getId(), dto.id());
            assertEquals(account.getName(), dto.name());
            assertEquals(account.getCurrentBalance(), dto.currentBalance());
            assertNotNull(dto.user());
            assertNotNull(dto.type());
            assertNotNull(dto.currency());
            assertNotNull(dto.bank());
        }

        @Test
        @DisplayName("should handle null optional fields")
        void toDto_shouldHandleNullOptionals() {
            // arrange
            Account account = Account.builder()
                    .id(1L)
                    .name("Minimal Account")
                    .build();

            // act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.user());
            assertNull(dto.type());
            assertNull(dto.currency());
            assertNull(dto.bank());
        }
    }
}
