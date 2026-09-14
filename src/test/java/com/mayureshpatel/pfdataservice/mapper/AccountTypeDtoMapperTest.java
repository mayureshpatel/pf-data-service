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
        // arrange
        Constructor<AccountTypeDtoMapper> constructor = AccountTypeDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        AccountTypeDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto(AccountType)")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // act
            AccountTypeDto result = AccountTypeDtoMapper.toDto((AccountType) null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            AccountType accountType = AccountType.builder()
                    .code("CHECKING")
                    .label("Checking Account")
                    .asset(true)
                    .sortOrder(1)
                    .active(true)
                    .icon("bank-icon")
                    .color("#000000")
                    .build();

            // act
            AccountTypeDto dto = AccountTypeDtoMapper.toDto(accountType);

            // assert & verify
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
            // arrange
            AccountType accountType = AccountType.builder()
                    .code("SAVINGS")
                    .label("Savings Account")
                    .asset(true)
                    .sortOrder(2)
                    .active(true)
                    .icon(null)
                    .color(null)
                    .build();

            // act
            AccountTypeDto dto = AccountTypeDtoMapper.toDto(accountType);

            // assert & verify
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
            // arrange
            AccountType accountType = AccountType.builder()
                    .code("CHECKING")
                    .label("Checking")
                    .build();
            List<AccountType> source = List.of(accountType);

            // act
            List<AccountTypeDto> result = AccountTypeDtoMapper.toDto(source);

            // assert & verify
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(accountType.getCode(), result.get(0).code());
        }

        @Test
        @DisplayName("should return empty list when source is empty")
        void toDto_shouldReturnEmptyListWhenSourceIsEmpty() {
            // act
            List<AccountTypeDto> result = AccountTypeDtoMapper.toDto(Collections.emptyList());

            // assert & verify
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
