package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryDtoMapper Unit Tests")
class CategoryDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // arrange
        Constructor<CategoryDtoMapper> constructor = CategoryDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // act
        CategoryDtoMapper instance = constructor.newInstance();

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
            CategoryDto result = CategoryDtoMapper.toDto(null);

            // assert & verify
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // arrange
            Category category = Category.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Dining")
                    .type("EXPENSE")
                    .parentId(null)
                    .icon("food-icon")
                    .color("#FF0000")
                    .build();

            // act
            CategoryDto dto = CategoryDtoMapper.toDto(category);

            // assert & verify
            assertNotNull(dto);
            assertEquals(category.getId(), dto.id());
            assertEquals(category.getUserId(), dto.userId());
            assertEquals(category.getName(), dto.name());
            assertEquals(CategoryType.EXPENSE, dto.type());
            assertNull(dto.parent());
            assertEquals(category.getIcon(), dto.icon());
            assertEquals(category.getColor(), dto.color());
        }

        @Test
        @DisplayName("should map parent ID when parentId is present")
        void toDto_shouldMapParentId() {
            // arrange
            Category parentCategory = Category.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Food")
                    .type("EXPENSE")
                    .build();

            Category category = Category.builder()
                    .id(2L)
                    .parentId(1L)
                    .parent(parentCategory)
                    .name("Restaurants")
                    .type("EXPENSE")
                    .build();

            // act
            CategoryDto dto = CategoryDtoMapper.toDto(category);

            // assert & verify
            assertNotNull(dto);
            assertNotNull(dto.parent());
            assertEquals(1L, dto.parent().id());
        }

        @Test
        @DisplayName("should handle null optional fields")
        void toDto_shouldHandleNullOptionals() {
            // arrange
            Category category = Category.builder()
                    .id(1L)
                    .name("Minimal")
                    .type(null)
                    .userId(null)
                    .icon(null)
                    .color(null)
                    .parentId(null)
                    .build();

            // act
            CategoryDto dto = CategoryDtoMapper.toDto(category);

            // assert & verify
            assertNotNull(dto);
            assertNull(dto.type());
            assertNull(dto.userId());
            assertNull(dto.icon());
            assertNull(dto.color());
            assertNull(dto.parent());
        }
    }
}
