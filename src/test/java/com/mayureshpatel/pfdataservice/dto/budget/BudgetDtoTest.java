package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BudgetDto Unit Tests")
class BudgetDtoTest {

    @Test
    @DisplayName("should correctly map all fields using constructor")
    void shouldPopulateFieldsViaConstructor() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetDto dto = new BudgetDto(
                1L,
                2L,
                category,
                new BigDecimal("500.00"),
                10,
                2024
        );

        assertEquals(1L, dto.id());
        assertEquals(2L, dto.userId());
        assertEquals(category, dto.category());
        assertEquals(new BigDecimal("500.00"), dto.amount());
        assertEquals(10, dto.month());
        assertEquals(2024, dto.year());
    }

    @Test
    @DisplayName("should correctly map all fields using builder")
    void shouldPopulateFieldsViaBuilder() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetDto dto = BudgetDto.builder()
                .id(1L)
                .userId(2L)
                .category(category)
                .amount(new BigDecimal("500.00"))
                .month(10)
                .year(2024)
                .build();

        assertEquals(1L, dto.id());
        assertEquals(2L, dto.userId());
        assertEquals(category, dto.category());
        assertEquals(new BigDecimal("500.00"), dto.amount());
        assertEquals(10, dto.month());
        assertEquals(2024, dto.year());
    }
}
