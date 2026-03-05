package com.mayureshpatel.pfdataservice.dto.transaction;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CategoryTransactionsDto Structure Tests")
class CategoryTransactionsDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        CategoryDto category = CategoryDto.builder().id(1L).name("Food").build();
        Integer count = 10;

        CategoryTransactionsDto dto = new CategoryTransactionsDto(category, count);

        assertEquals(category, dto.category());
        assertEquals(count, dto.transactionCount());
    }
}
