package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BudgetStatusDto Unit Tests")
class BudgetStatusDtoTest {

    @Test
    @DisplayName("should correctly map all fields using constructor")
    void shouldPopulateFieldsViaConstructor() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetStatusDto dto = new BudgetStatusDto(
                category,
                new BigDecimal("500.00"),
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                40.0
        );

        assertEquals(category, dto.category());
        assertEquals(new BigDecimal("500.00"), dto.budgetedAmount());
        assertEquals(new BigDecimal("200.00"), dto.spentAmount());
        assertEquals(new BigDecimal("300.00"), dto.remainingAmount());
        assertEquals(40.0, dto.percentageUsed());
    }

    @Test
    @DisplayName("should correctly map all fields using builder")
    void shouldPopulateFieldsViaBuilder() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetStatusDto dto = BudgetStatusDto.builder()
                .category(category)
                .budgetedAmount(new BigDecimal("500.00"))
                .spentAmount(new BigDecimal("200.00"))
                .remainingAmount(new BigDecimal("300.00"))
                .percentageUsed(40.0)
                .build();

        assertEquals(category, dto.category());
        assertEquals(new BigDecimal("500.00"), dto.budgetedAmount());
        assertEquals(new BigDecimal("200.00"), dto.spentAmount());
        assertEquals(new BigDecimal("300.00"), dto.remainingAmount());
        assertEquals(40.0, dto.percentageUsed());
    }
}
