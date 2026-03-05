package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryRuleDtoMapper Unit Tests")
class CategoryRuleDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<CategoryRuleDtoMapper> constructor = CategoryRuleDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        CategoryRuleDtoMapper instance = constructor.newInstance();

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
            CategoryRuleDto result = CategoryRuleDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            User user = User.builder().id(100L).build();
            Category category = Category.builder().id(50L).name("Dining").type("EXPENSE").build();
            CategoryRule rule = CategoryRule.builder()
                    .id(1L)
                    .user(user)
                    .keyword("MCDONALDS")
                    .priority(1)
                    .category(category)
                    .build();

            // Act
            CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

            // Assert
            assertNotNull(dto);
            assertEquals(rule.getId(), dto.id());
            assertEquals(user.getId(), dto.userId());
            assertEquals(rule.getKeyword(), dto.keyword());
            assertEquals(rule.getPriority(), dto.priority());
            assertNotNull(dto.category());
            assertEquals(category.getId(), dto.category().id());
        }

        @Test
        @DisplayName("should handle null user and category")
        void toDto_shouldHandleNulls() {
            // Arrange
            CategoryRule rule = CategoryRule.builder()
                    .id(1L)
                    .keyword("TEST")
                    .priority(10)
                    .user(null)
                    .category(null)
                    .build();

            // Act
            CategoryRuleDto dto = CategoryRuleDtoMapper.toDto(rule);

            // Assert
            assertNotNull(dto);
            assertNull(dto.userId());
            assertNull(dto.category());
        }
    }
}
