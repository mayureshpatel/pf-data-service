package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.Account;
import com.mayureshpatel.pfdataservice.dto.account.AccountDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gold Standard examples for Mapper unit testing.
 * Demonstrates testing for full mapping, partial mapping, and null handling using @Nested organization.
 */
@DisplayName("Mapper Gold Standard Tests")
class MapperGoldStandardTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<AccountDtoMapper> constructor = AccountDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        AccountDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // Act
            AccountDto result = AccountDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            Account account = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Checking")
                    .typeCode("CHECKING")
                    .currentBalance(new BigDecimal("1500.00"))
                    .currencyCode("USD")
                    .bankCode("CAPITAL_ONE")
                    .build();

            // Act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // Assert
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
            // Arrange
            Account account = Account.builder()
                    .id(1L)
                    .name("Minimal Account")
                    .build();

            // Act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // Assert
            assertNotNull(dto);
            assertNull(dto.user());
            assertNull(dto.type());
            assertNull(dto.currency());
            assertNull(dto.bank());
        }
    }
}
