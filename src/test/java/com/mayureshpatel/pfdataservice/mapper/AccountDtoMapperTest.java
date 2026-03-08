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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountDtoMapper Unit Tests")
class AccountDtoMapperTest {

    @Test
    @DisplayName("Private constructor should not be accessible but can be called for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<AccountDtoMapper> constructor = AccountDtoMapper.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        // Act
        AccountDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("toDto mapping logic")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when account is null")
        void toDto_shouldReturnNullWhenAccountIsNull() {
            // Arrange
            Account account = null;

            // Act
            AccountDto result = AccountDtoMapper.toDto(account);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when account is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            Account account = Account.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Checking")
                    .type(AccountType.builder().code("CHECKING").build())
                    .currentBalance(new BigDecimal("1500.00"))
                    .currency(Currency.builder().code("USD").build())
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
            assertEquals(account.getUserId(), dto.user().id());

            assertNotNull(dto.type());
            assertEquals(account.getType().getCode(), dto.type().code());

            assertNotNull(dto.currency());
            assertEquals(account.getCurrency().getCode(), dto.currency().code());

            assertNotNull(dto.bank());
            assertEquals("CAPITAL_ONE", dto.bank().name());
        }

        @Test
        @DisplayName("should handle null optional fields")
        void toDto_shouldHandleNullOptionals() {
            // Arrange
            Account account = Account.builder()
                    .id(1L)
                    .name("Minimal Account")
                    .userId(null)
                    .type(null)
                    .currency(null)
                    .bankCode(null)
                    .build();

            // Act
            AccountDto dto = AccountDtoMapper.toDto(account);

            // Assert
            assertNotNull(dto);
            assertEquals(account.getId(), dto.id());
            assertEquals(account.getName(), dto.name());
            assertNull(dto.user());
            assertNull(dto.type());
            assertNull(dto.currency());
            assertNull(dto.bank());
        }
    }
}
