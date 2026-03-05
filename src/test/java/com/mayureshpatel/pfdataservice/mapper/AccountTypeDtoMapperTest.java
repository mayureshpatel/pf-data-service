package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.account.AccountType;
import com.mayureshpatel.pfdataservice.dto.account.AccountTypeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountTypeDtoMapper Unit Tests")
class AccountTypeDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<AccountTypeDtoMapper> constructor = AccountTypeDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        AccountTypeDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto(AccountType)")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // Act
            AccountTypeDto result = AccountTypeDtoMapper.toDto((AccountType) null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            AccountType accountType = AccountType.builder()
                    .code("CHECKING")
                    .label("Checking Account")
                    .asset(true)
                    .sortOrder(1)
                    .active(true)
                    .icon("bank-icon")
                    .color("#000000")
                    .build();

            // Act
            AccountTypeDto dto = AccountTypeDtoMapper.toDto(accountType);

            // Assert
            assertNotNull(dto);
            assertEquals(accountType.getCode(), dto.code());
            assertEquals(accountType.getLabel(), dto.label());
            assertEquals(accountType.isAsset(), dto.isAsset());
            assertEquals(accountType.getSortOrder(), dto.sortOrder());
            assertEquals(accountType.isActive(), dto.isActive());
            assertEquals(accountType.getIcon(), dto.icon());
            assertEquals(accountType.getColor(), dto.color());
        }

        @Test
        @DisplayName("should handle null optional fields (icon and color)")
        void toDto_shouldHandleNullOptionals() {
            // Arrange
            AccountType accountType = AccountType.builder()
                    .code("SAVINGS")
                    .label("Savings Account")
                    .asset(true)
                    .sortOrder(2)
                    .active(true)
                    .icon(null)
                    .color(null)
                    .build();

            // Act
            AccountTypeDto dto = AccountTypeDtoMapper.toDto(accountType);

            // Assert
            assertNotNull(dto);
            assertNull(dto.icon());
            assertNull(dto.color());
        }
    }

    @Nested
    @DisplayName("Method: toDto(List<AccountType>)")
    class ToDtoListMappingTests {

        @Test
        @DisplayName("should map list of account types")
        void toDto_shouldMapList() {
            // Arrange
            AccountType accountType = AccountType.builder()
                    .code("CHECKING")
                    .label("Checking")
                    .build();
            List<AccountType> source = List.of(accountType);

            // Act
            List<AccountTypeDto> result = AccountTypeDtoMapper.toDto(source);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(accountType.getCode(), result.get(0).code());
        }

        @Test
        @DisplayName("should return empty list when source is empty")
        void toDto_shouldReturnEmptyListWhenSourceIsEmpty() {
            // Act
            List<AccountTypeDto> result = AccountTypeDtoMapper.toDto(Collections.emptyList());

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
