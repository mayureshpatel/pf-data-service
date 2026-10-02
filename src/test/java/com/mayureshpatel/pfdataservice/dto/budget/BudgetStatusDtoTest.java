package com.mayureshpatel.pfdataservice.dto.budget;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies {@code BudgetStatusDto}'s record accessors map their arguments 1:1, whether built via its canonical constructor or its builder. */
@DisplayName("BudgetStatusDto Unit Tests")
class BudgetStatusDtoTest {

    private static final String BUDGETED_AMOUNT = "500.00";
    private static final String SPENT_AMOUNT = "200.00";
    private static final String REMAINING_AMOUNT = "300.00";

    @Test
    @DisplayName("should correctly map all fields using constructor")
    void shouldPopulateFieldsViaConstructor() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetStatusDto dto = new BudgetStatusDto(
                category,
                new BigDecimal(BUDGETED_AMOUNT),
                new BigDecimal(SPENT_AMOUNT),
                new BigDecimal(REMAINING_AMOUNT),
                40.0
        );

        assertEquals(category, dto.category());
        assertEquals(new BigDecimal(BUDGETED_AMOUNT), dto.budgetedAmount());
        assertEquals(new BigDecimal(SPENT_AMOUNT), dto.spentAmount());
        assertEquals(new BigDecimal(REMAINING_AMOUNT), dto.remainingAmount());
        assertEquals(40.0, dto.percentageUsed());
    }

    @Test
    @DisplayName("should correctly map all fields using builder")
    void shouldPopulateFieldsViaBuilder() {
        CategoryDto category = new CategoryDto(1L, 2L, "Food", null, null, "icon", "color");
        BudgetStatusDto dto = BudgetStatusDto.builder()
                .category(category)
                .budgetedAmount(new BigDecimal(BUDGETED_AMOUNT))
                .spentAmount(new BigDecimal(SPENT_AMOUNT))
                .remainingAmount(new BigDecimal(REMAINING_AMOUNT))
                .percentageUsed(40.0)
                .build();

        assertEquals(category, dto.category());
        assertEquals(new BigDecimal(BUDGETED_AMOUNT), dto.budgetedAmount());
        assertEquals(new BigDecimal(SPENT_AMOUNT), dto.spentAmount());
        assertEquals(new BigDecimal(REMAINING_AMOUNT), dto.remainingAmount());
        assertEquals(40.0, dto.percentageUsed());
    }
}
