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

        @Test
        @DisplayName("PF-313: should break a full tie (same priority AND same keyword length) by id "
                + "ascending, so ordering stays deterministic rather than falling to whatever order "
                + "Postgres happens to return")
        void shouldBreakFullTieByIdAscending() {
            // arrange -- two new rules, same priority, same keyword length ("AAAA" / "ZZZZ").
            // PF-314: ids are no longer caller-supplied (insertAndReturnId generates them), so the
            // tie-break is exercised via real insertion order instead of hand-picked literals.
            CategoryRule ruleA = CategoryRule.builder()
                    .keyword("AAAA")
                    .priority(5)
                    .category(Category.builder().id(7L).build())
                    .user(User.builder().id(USER_1).build())
                    .build();
            CategoryRule ruleB = CategoryRule.builder()
                    .keyword("ZZZZ")
                    .priority(5)
                    .category(Category.builder().id(7L).build())
                    .user(User.builder().id(USER_1).build())
                    .build();
            Long firstInsertedId = repository.insertAndReturnId(ruleA);
            Long secondInsertedId = repository.insertAndReturnId(ruleB);

            // act
            List<CategoryRule> result = repository.findByUserId(USER_1);

            // assert & verify -- both tied rules land consecutively, earlier-inserted (lower) id first
            List<CategoryRule> tied = result.stream()
                    .filter(r -> r.getId().equals(firstInsertedId) || r.getId().equals(secondInsertedId))
                    .toList();
            assertEquals(2, tied.size());
            assertEquals(firstInsertedId, tied.get(0).getId());
            assertEquals(secondInsertedId, tied.get(1).getId());
        }
    }

    @Nested
    @DisplayName("Status & Counts")
    class StatusTests {
        @Test
        @DisplayName("should count rules for a specific category")
        void shouldCountByCategoryId() {
            // Act -- category 7 (Groceries) has exactly one baseline rule (WHOLEFDS)
            long count = repository.countByCategoryId(7L);

            // Assert
            assertEquals(1, count);
        }

        @Test
        @DisplayName("should count zero rules for a category with none")
        void shouldCountByCategoryIdZeroWhenNoRules() {
            // Act -- category 6 (Rent) has no baseline category rule
            long count = repository.countByCategoryId(6L);

            // Assert
            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("PF-314: should insert a new category rule and return a real generated id "
                + "(previously threw DataIntegrityViolationException on every call -- the caller "
                + "never supplied an id, and explicit NULL bypasses a BIGSERIAL column's default "
                + "instead of triggering it)")
        void shouldInsert() {
            // arrange -- amount range included to prove it round-trips through real NUMERIC(19,2)
            // columns, not just through in-memory objects
            CategoryRule rule = CategoryRule.builder()
                    .keyword("TEST")
                    .priority(1)
                    .category(Category.builder().id(7L).build()) // Groceries
                    .user(User.builder().id(USER_1).build())
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();

            // act
            Long generatedId = repository.insertAndReturnId(rule);

            // assert & verify
            assertNotNull(generatedId);
            assertTrue(generatedId > 0);
            CategoryRule persisted = repository.findById(generatedId).orElseThrow();
            assertEquals("TEST", persisted.getKeyword());
            assertEquals(1, persisted.getPriority());
            assertEquals(new java.math.BigDecimal("5.00"), persisted.getMinAmount());
            assertEquals(new java.math.BigDecimal("20.00"), persisted.getMaxAmount());
        }

        @Test
        @DisplayName("PF-314: should actually persist an update (previously threw "
                + "UnsupportedOperationException on every call -- the repository never overrode "
                + "the interface's default, unconditionally-throwing update())")
        void shouldUpdate() {
            // arrange
            CategoryRule rule = CategoryRule.builder()
                    .keyword("ORIGINAL")
                    .priority(1)
                    .category(Category.builder().id(7L).build())
                    .user(User.builder().id(USER_1).build())
                    .build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updated = rule.toBuilder()
                    .id(id)
                    .keyword("UPDATED")
                    .priority(99)
                    .minAmount(new java.math.BigDecimal("10.00"))
                    .maxAmount(new java.math.BigDecimal("50.00"))
                    .build();

            // act
            int rows = repository.update(updated);

            // assert & verify
            assertEquals(1, rows);
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals("UPDATED", persisted.getKeyword());
            assertEquals(99, persisted.getPriority());
            assertEquals(new java.math.BigDecimal("10.00"), persisted.getMinAmount());
            assertEquals(new java.math.BigDecimal("50.00"), persisted.getMaxAmount());
        }

        @Test
        @DisplayName("PF-314: update should affect zero rows when the userId doesn't match -- the "
                + "SQL-level ownership scope (defense-in-depth's last layer, matching the pattern "
                + "already established for accounts and merchants), not just the Controller's "
                + "@PreAuthorize or the Service's own findById+equals check")
        void shouldNotUpdateWhenUserIdDoesNotMatch() {
            // arrange
            CategoryRule rule = CategoryRule.builder()
                    .keyword("ORIGINAL")
                    .priority(1)
                    .category(Category.builder().id(7L).build())
                    .user(User.builder().id(USER_1).build())
                    .build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updateAttempt = rule.toBuilder()
                    .id(id)
                    .keyword("SHOULD_NOT_APPLY")
                    .user(User.builder().id(999L).build()) // wrong user
                    .build();

            // act
            int rows = repository.update(updateAttempt);

            // assert & verify -- no rows affected, original data untouched
            assertEquals(0, rows);
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals("ORIGINAL", persisted.getKeyword());
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
