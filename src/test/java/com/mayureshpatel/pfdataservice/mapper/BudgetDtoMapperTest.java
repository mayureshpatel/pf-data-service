package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.budget.Budget;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.budget.BudgetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BudgetDtoMapper Unit Tests")
class BudgetDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<BudgetDtoMapper> constructor = BudgetDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        BudgetDtoMapper instance = constructor.newInstance();

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
            BudgetDto result = BudgetDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            Budget budget = Budget.builder()
                    .id(1L)
                    .userId(100L)
                    .category(Category.builder().id(50L).build())
                    .amount(new BigDecimal("1000.00"))
                    .month(10)
                    .year(2023)
                    .build();

            // Act
            BudgetDto dto = BudgetDtoMapper.toDto(budget);

            // Assert
            assertNotNull(dto);
            assertEquals(budget.getId(), dto.id());
            assertEquals(budget.getUserId(), dto.userId());
            assertEquals(budget.getAmount(), dto.amount());
            assertEquals(budget.getMonth(), dto.month());
            assertEquals(budget.getYear(), dto.year());
            assertNotNull(dto.category());
            assertEquals(budget.getCategory().getId(), dto.category().id());
        }

        @Test
        @DisplayName("should handle null userId and categoryId")
        void toDto_shouldHandleNullIds() {
            // Arrange
            Budget budget = Budget.builder()
                    .id(1L)
                    .userId(null)
                    .category(null)
                    .amount(new BigDecimal("1000.00"))
                    .month(10)
                    .year(2023)
                    .build();

            // Act
            BudgetDto dto = BudgetDtoMapper.toDto(budget);

            // Assert
            assertNotNull(dto);
            assertNull(dto.userId());
            assertNull(dto.category());
        }
    }
}
