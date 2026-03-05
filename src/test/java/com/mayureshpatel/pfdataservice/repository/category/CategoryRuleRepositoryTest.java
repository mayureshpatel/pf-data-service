package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(CategoryRuleRepository.class)
@DisplayName("CategoryRuleRepository Integration Tests (PostgreSQL)")
class CategoryRuleRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRuleRepository repository;

    private static final Long USER_1 = 1L;

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all rules by user ID ordered by priority desc and length desc")
        void shouldFindByUserId() {
            // Act
            List<CategoryRule> result = repository.findByUserId(USER_1);

            // Assert
            assertEquals(3, result.size());
            // baseline priorities: WHOLEFDS=10, SHELL=5, CAFE=1
            assertEquals("WHOLEFDS", result.get(0).getKeyword());
            assertEquals("SHELL", result.get(1).getKeyword());
            assertEquals("CAFE", result.get(2).getKeyword());
            
            // Check enriched category data
            assertNotNull(result.get(0).getCategory().getName());
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a new category rule or update on conflict")
        void shouldInsertOrUpdate() {
            // Arrange
            CategoryRule rule = CategoryRule.builder()
                    .id(999L) // manual ID to test conflict
                    .keyword("TEST")
                    .priority(1)
                    .category(Category.builder().id(7L).build()) // Groceries
                    .user(User.builder().id(USER_1).build())
                    .build();

            // Act - Insert
            int insertRows = repository.insert(rule);

            // Assert - Insert
            assertEquals(1, insertRows);

            // Act - Update (Conflict)
            CategoryRule updatedRule = rule.toBuilder().keyword("UPDATED_TEST").priority(99).build();
            int updateRows = repository.insert(updatedRule);

            // Assert - Update
            assertEquals(1, updateRows);
            List<CategoryRule> rules = repository.findByUserId(USER_1);
            assertTrue(rules.stream().anyMatch(r -> r.getKeyword().equals("UPDATED_TEST") && r.getPriority() == 99));
        }

        @Test
        @DisplayName("should delete a category rule by ID and UserID")
        void shouldDeleteById() {
            // Arrange
            List<CategoryRule> existing = repository.findByUserId(USER_1);
            Long idToDelete = existing.get(0).getId();

            // Act
            int rows = repository.deleteById(idToDelete, USER_1);

            // Assert
            assertEquals(1, rows);
            List<CategoryRule> afterDelete = repository.findByUserId(USER_1);
            assertEquals(2, afterDelete.size());
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for insecure deleteById")
        void shouldThrowOnInsecureDelete() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }
    }
}
