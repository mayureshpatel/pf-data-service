package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(CategoryRepository.class)
@DisplayName("CategoryRepository Integration Tests (PostgreSQL)")
class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long CAT_FOOD = 2L; // Parent category in baseline
    private static final Long CAT_GROCERIES = 7L; // Sub-category of Food

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all categories")
        void shouldFindAll() {
            // Act
            List<Category> result = categoryRepository.findAll();

            // Assert
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 11); // 10 from user 1, 1 from user 2
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // Act
            Optional<Category> result = categoryRepository.findById(CAT_FOOD);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("Food", result.get().getName());
        }

        @Test
        @DisplayName("should find by user ID")
        void shouldFindByUserId() {
            // Act
            List<Category> result = categoryRepository.findByUserId(USER_1);

            // Assert
            assertEquals(10, result.size());
            assertTrue(result.stream().allMatch(c -> c.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find all with parent enrichment")
        void shouldFindAllWithParent() {
            // Act
            List<Category> result = categoryRepository.findAllWIthParent(USER_1);

            // Assert
            assertFalse(result.isEmpty());
            Category groceries = result.stream()
                    .filter(c -> c.getId().equals(CAT_GROCERIES))
                    .findFirst()
                    .orElseThrow();
            
            assertNotNull(groceries.getParent());
            assertEquals("Food", groceries.getParent().getName());
        }

        @Test
        @DisplayName("should find all sub-categories only")
        void shouldFindSubCategories() {
            // Act
            List<Category> result = categoryRepository.findAllSubCategories(USER_1);

            // Assert
            assertEquals(5, result.size());
            assertTrue(result.stream().allMatch(c -> c.getParentId() != null));
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new category")
        void shouldInsert() {
            // Arrange
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Subscriptions")
                    .color("#ABCDEF")
                    .icon("tv")
                    .type("EXPENSE")
                    .userId(USER_1)
                    .build();

            // Act
            int rows = categoryRepository.insert(request);

            // Assert
            assertEquals(1, rows);
            assertEquals(11, categoryRepository.count(USER_1));
        }

        @Test
        @DisplayName("should insert a sub-category")
        void shouldInsertSubCategory() {
            // Arrange
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Streaming")
                    .parentId(CAT_FOOD) // Arbitrary parent for test
                    .userId(USER_1)
                    .type("EXPENSE")
                    .build();

            // Act
            int rows = categoryRepository.insert(request);

            // Assert
            assertEquals(1, rows);
            List<Category> subs = categoryRepository.findAllSubCategories(USER_1);
            assertTrue(subs.stream().anyMatch(c -> c.getName().equals("Streaming")));
        }

        @Test
        @DisplayName("should update an existing category")
        void shouldUpdate() {
            // Arrange
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(CAT_FOOD)
                    .userId(USER_1)
                    .name("Food & Dining")
                    .color("#000000")
                    .type("EXPENSE")
                    .build();

            // Act
            int rows = categoryRepository.update(request);

            // Assert
            assertEquals(1, rows);
            Category updated = categoryRepository.findById(CAT_FOOD).orElseThrow();
            assertEquals("Food & Dining", updated.getName());
            assertEquals("#000000", updated.getColor());
        }

        @Test
        @DisplayName("should delete a category")
        void shouldDelete() {
            // Arrange
            Category toDelete = Category.builder().id(CAT_GROCERIES).userId(USER_1).build();

            // Act
            int rows = categoryRepository.delete(toDelete);

            // Assert
            assertEquals(1, rows);
            assertTrue(categoryRepository.findById(CAT_GROCERIES).isEmpty());
        }

        @Test
        @DisplayName("should return 0 when deleting category with no ID")
        void shouldHandleNoIdDelete() {
            // Act
            int rows = categoryRepository.delete(Category.builder().build());

            // Assert
            assertEquals(0, rows);
        }

        @Test
        @DisplayName("should throw error on deleteById")
        void shouldThrowOnDeleteById() {
            assertThrows(UnsupportedOperationException.class, () -> categoryRepository.deleteById(1L));
        }
    }

    @Test
    @DisplayName("should count categories for a user")
    void shouldCount() {
        // Act
        long count = categoryRepository.count(USER_1);

        // Assert
        assertEquals(10, count);
    }
}
