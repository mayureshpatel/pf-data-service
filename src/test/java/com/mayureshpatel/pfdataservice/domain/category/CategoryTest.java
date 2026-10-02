package com.mayureshpatel.pfdataservice.domain.category;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@link Category}'s Lombok-generated builder and ID-based {@code equals}/{@code hashCode}, plus {@link Category#isSubCategory()} in {@link IsSubCategoryTests}. */
@DisplayName("Category Domain Object Tests")
class CategoryTest {

    /** {@link Category#isSubCategory()} treats both a null and a zero {@code parentId} as "no parent," not just null. */
    @Nested
    @DisplayName("isSubCategory logic")
    class IsSubCategoryTests {

        @Test
        @DisplayName("should return false when parentId is null")
        void shouldReturnFalseWhenParentIdIsNull() {
            Category category = Category.builder().parentId(null).build();
            assertFalse(category.isSubCategory());
        }

        @Test
        @DisplayName("should return false when parentId is 0")
        void shouldReturnFalseWhenParentIdIsZero() {
            Category category = Category.builder().parentId(0L).build();
            assertFalse(category.isSubCategory());
        }

        @Test
        @DisplayName("should return true when parentId is positive")
        void shouldReturnTrueWhenParentIdIsPositive() {
            Category category = Category.builder().parentId(10L).build();
            assertTrue(category.isSubCategory());
        }
    }

    @Test
    @DisplayName("Builder should correctly populate all fields")
    void builder_shouldPopulateFields() {
        Category category = Category.builder()
                .id(1L)
                .userId(100L)
                .name("Groceries")
                .type("EXPENSE")
                .parentId(5L)
                .color("#FF0000")
                .icon("shopping_cart")
                .build();

        assertEquals(1L, category.getId());
        assertEquals(100L, category.getUserId());
        assertEquals("Groceries", category.getName());
        assertEquals("EXPENSE", category.getType());
        assertEquals(5L, category.getParentId());
        assertEquals("#FF0000", category.getColor());
        assertEquals("shopping_cart", category.getIcon());
    }

    @Test
    @DisplayName("Equality should be based on ID")
    void equality_shouldBeBasedOnId() {
        Category c1 = Category.builder().id(1L).name("A").build();
        Category c2 = Category.builder().id(1L).name("B").build();
        Category c3 = Category.builder().id(2L).build();

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
