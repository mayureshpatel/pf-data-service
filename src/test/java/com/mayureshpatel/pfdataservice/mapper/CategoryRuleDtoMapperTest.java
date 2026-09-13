package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryRuleDtoMapper Unit Tests")
class CategoryRuleDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<CategoryRuleDtoMapper> constructor = CategoryRuleDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        CategoryRuleDtoMapper instance = constructor.newInstance();

        // assert & verify
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // act
            CategoryRuleDto result = CategoryRuleDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            User user = User.builder().id(100L).build();
            Category category = Category.builder().id(50L).name("Dining").type("EXPENSE").build();
            CategoryRule rule = CategoryRule.builder()
                    .id(1L)
                    .user(user)
                    .keywords(List.of("MCDONALDS"))
                    .matchType(MatchType.OR)
                    .priority(1)
                    .category(category)
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .maxAmount(new java.math.BigDecimal("100.00"))
                    .build();

            // act
            CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

            // assert & verify
            assertNotNull(dto);
            assertEquals(rule.getId(), dto.id());
            assertEquals(user.getId(), dto.userId());
            assertEquals(rule.getKeywords(), dto.keywords());
            assertEquals(rule.getMatchType(), dto.matchType());
            assertEquals(rule.getPriority(), dto.priority());
            assertNotNull(dto.category());
            assertEquals(category.getId(), dto.category().id());
            assertEquals(rule.getMinAmount(), dto.minAmount());
            assertEquals(rule.getMaxAmount(), dto.maxAmount());
        }

        @Test
        @DisplayName("should handle null user and category")
        void toDto_shouldHandleNulls() {
            // arrange
            CategoryRule rule = CategoryRule.builder()
                    .id(1L)
                    .keywords(List.of("TEST"))
                    .priority(10)
                    .user(null)
                    .category(null)
                    .build();

            // act
            CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.userId());
            assertNull(dto.category());
        }
    }
}
