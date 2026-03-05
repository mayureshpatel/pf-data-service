package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CategoryGroupDto Tests")
class CategoryGroupDtoTest {

    @Test
    @DisplayName("should correctly map all fields")
    void shouldPopulateFields() {
        CategoryDto item1 = new CategoryDto(1L, 1L, "Groceries", CategoryType.EXPENSE, null, "icon", "color");
        CategoryDto item2 = new CategoryDto(2L, 1L, "Dining", CategoryType.EXPENSE, null, "icon", "color");
        List<CategoryDto> items = List.of(item1, item2);
        
        CategoryGroupDto dto = new CategoryGroupDto("Expenses", 1L, items);

        assertEquals("Expenses", dto.groupLabel());
        assertEquals(1L, dto.groupId());
        assertEquals(items, dto.items());
        assertEquals(2, dto.items().size());
    }
}
