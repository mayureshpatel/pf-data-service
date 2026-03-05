package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("CategoryRuleDto Tests")
class CategoryRuleDtoTest {

    @Test
    @DisplayName("should correctly map all fields via builder")
    void shouldPopulateFieldsViaBuilder() {
        CategoryDto category = new CategoryDto(1L, 1L, "Groceries", CategoryType.EXPENSE, null, "icon", "color");
        CategoryRuleDto dto = CategoryRuleDto.builder()
                .id(1L)
                .userId(1L)
                .keyword("PUBLIX")
                .priority(1)
                .category(category)
                .build();

        assertEquals(1L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("PUBLIX", dto.keyword());
        assertEquals(1, dto.priority());
        assertEquals(category, dto.category());
    }

    @Test
    @DisplayName("should correctly map all fields via constructor")
    void shouldPopulateFieldsViaConstructor() {
        CategoryDto category = new CategoryDto(1L, 1L, "Groceries", CategoryType.EXPENSE, null, "icon", "color");
        CategoryRuleDto dto = new CategoryRuleDto(1L, 1L, "PUBLIX", 1, category);

        assertEquals(1L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("PUBLIX", dto.keyword());
        assertEquals(1, dto.priority());
        assertEquals(category, dto.category());
    }
}
