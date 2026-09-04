package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                .minAmount(new BigDecimal("5.00"))
                .maxAmount(new BigDecimal("100.00"))
                .build();

        assertEquals(1L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("PUBLIX", dto.keyword());
        assertEquals(1, dto.priority());
        assertEquals(category, dto.category());
        assertEquals(new BigDecimal("5.00"), dto.minAmount());
        assertEquals(new BigDecimal("100.00"), dto.maxAmount());
    }

    @Test
    @DisplayName("should correctly map all fields via constructor")
    void shouldPopulateFieldsViaConstructor() {
        CategoryDto category = new CategoryDto(1L, 1L, "Groceries", CategoryType.EXPENSE, null, "icon", "color");
        CategoryRuleDto dto = new CategoryRuleDto(1L, 1L, "PUBLIX", 1, category, null, null);

        assertEquals(1L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals("PUBLIX", dto.keyword());
        assertEquals(1, dto.priority());
        assertEquals(category, dto.category());
        assertNull(dto.minAmount());
        assertNull(dto.maxAmount());
    }
}
