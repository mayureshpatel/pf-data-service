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
            // act
            List<Category> result = categoryRepository.findAll();

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.size() >= 11); // 10 from user 1, 1 from user 2
        }

        @Test
        @DisplayName("should find by ID")
        void shouldFindById() {
            // act
            Optional<Category> result = categoryRepository.findById(CAT_FOOD);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals("Food", result.get().getName());
        }

        @Test
        @DisplayName("should find by user ID")
        void shouldFindByUserId() {
            // act
            List<Category> result = categoryRepository.findByUserId(USER_1);

            // assert & verify
            assertEquals(10, result.size());
            assertTrue(result.stream().allMatch(c -> c.getUserId().equals(USER_1)));
        }

        @Test
        @DisplayName("should find all parent categories")
        void shouldFindAllParentCategories() {
            // act
            List<Category> result = categoryRepository.findAllParentCategories(USER_1);

            // assert & verify
            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(c -> c.getParentId() != null));
        }

        @Test
        @DisplayName("should find all sub-categories only")
        void shouldFindSubCategories() {
            // act
            List<Category> result = categoryRepository.findAllSubCategories(USER_1);

            // assert & verify
            assertEquals(5, result.size());
            assertTrue(result.stream().allMatch(c -> c.getParentId() != null));
        }
    }

    @Nested
    @DisplayName("Status & Counts")
    class StatusTests {
        @Test
        @DisplayName("should count subcategories for a parent category")
        void shouldCountByParentId() {
            // act -- category 1 (Housing) has exactly one baseline subcategory (Rent, id 6)
            long count = categoryRepository.countByParentId(1L);

            // assert & verify
            assertEquals(1, count);
        }

        @Test
        @DisplayName("should count zero subcategories for a category with none")
        void shouldCountByParentIdZeroWhenNoSubcategories() {
            // act -- category 6 (Rent) is itself a subcategory with no children of its own
            long count = categoryRepository.countByParentId(6L);

            // assert & verify
            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new category")
        void shouldInsert() {
            // arrange
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Subscriptions")
                    .color("#ABCDEF")
                    .icon("tv")
                    .type("EXPENSE")
                    .userId(USER_1)
                    .build();

            // act
            int newId = categoryRepository.insert(request);

            // assert & verify -- must be the real generated id, not update()'s rows-affected count (always
            // 1 on a successful single-row insert, which would coincidentally collide with
            // baseline category 1 and mask the bug this regresses against)
            assertEquals(11, categoryRepository.count(USER_1));
            Category inserted = categoryRepository.findById((long) newId).orElseThrow();
            assertEquals("Subscriptions", inserted.getName());
            assertEquals(USER_1, inserted.getUserId());
        }

        @Test
        @DisplayName("should insert a sub-category")
        void shouldInsertSubCategory() {
            // arrange
            CategoryCreateRequest request = CategoryCreateRequest.builder()
                    .name("Streaming")
                    .parentId(CAT_FOOD) // Arbitrary parent for test
                    .userId(USER_1)
                    .type("EXPENSE")
                    .build();

            // act
            int newId = categoryRepository.insert(request);

            // assert & verify
            Category inserted = categoryRepository.findById((long) newId).orElseThrow();
            assertEquals("Streaming", inserted.getName());
            assertEquals(CAT_FOOD, inserted.getParentId());
            List<Category> subs = categoryRepository.findAllSubCategories(USER_1);
            assertTrue(subs.stream().anyMatch(c -> c.getName().equals("Streaming")));
        }

        @Test
        @DisplayName("should update an existing category")
        void shouldUpdate() {
            // arrange
            CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                    .id(CAT_FOOD)
                    .userId(USER_1)
                    .name("Food & Dining")
                    .color("#000000")
                    .type("EXPENSE")
                    .build();

            // act
            int rows = categoryRepository.update(request);

            // assert & verify
            assertEquals(1, rows);
            Category updated = categoryRepository.findById(CAT_FOOD).orElseThrow();
            assertEquals("Food & Dining", updated.getName());
            assertEquals("#000000", updated.getColor());
        }

        @Test
        @DisplayName("should delete a category")
        void shouldDelete() {
            // arrange
            Category toDelete = Category.builder().id(CAT_GROCERIES).userId(USER_1).build();

            // act
            int rows = categoryRepository.delete(toDelete);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(categoryRepository.findById(CAT_GROCERIES).isEmpty());
        }

        @Test
        @DisplayName("should return 0 when deleting category with no ID")
        void shouldHandleNoIdDelete() {
            // act
            int rows = categoryRepository.delete(Category.builder().build());

            // assert & verify
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
        // act
        long count = categoryRepository.count(USER_1);

        // assert & verify
        assertEquals(10, count);
    }
}
