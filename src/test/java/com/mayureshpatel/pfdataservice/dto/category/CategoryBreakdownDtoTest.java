package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CategoryBreakdownDto Tests")
class CategoryBreakdownDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        CategoryDto category = new CategoryDto(1L, 1L, "Groceries", CategoryType.EXPENSE, null, "icon", "color");
        BigDecimal total = new BigDecimal("100.50");
        CategoryBreakdownDto dto = new CategoryBreakdownDto(category, total);

        assertEquals(category, dto.category());
        assertEquals(total, dto.total());
    }
}
