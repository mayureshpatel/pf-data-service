package com.mayureshpatel.pfdataservice.repository.category;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
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

    private CategoryRule.CategoryRuleBuilder baseRuleBuilder() {
        return CategoryRule.builder()
                .priority(1)
                .matchType(MatchType.OR)
                .category(Category.builder().id(7L).build())
                .user(User.builder().id(USER_1).build());
    }

    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should find all rules by user ID ordered by priority desc and specificity desc")
        void shouldFindByUserId() {
            // Act
            List<CategoryRule> result = repository.findByUserId(USER_1);

            // Assert
            assertEquals(3, result.size());
            // baseline priorities: WHOLEFDS=10, SHELL=5, CAFE=1
            assertEquals(List.of("WHOLEFDS"), result.get(0).getKeywords());
            assertEquals(List.of("SHELL"), result.get(1).getKeywords());
            assertEquals(List.of("CAFE"), result.get(2).getKeywords());

            // Check enriched category data
            assertNotNull(result.get(0).getCategory().getName());
        }

        @Test
        @DisplayName("PF-313: should break a full tie (same priority AND same specificity) by id "
                + "ascending, so ordering stays deterministic rather than falling to whatever order "
                + "Postgres happens to return")
        void shouldBreakFullTieByIdAscending() {
            // arrange -- two new rules, same priority, same keyword length ("AAAA" / "ZZZZ").
            // PF-314: ids are no longer caller-supplied (insertAndReturnId generates them), so the
            // tie-break is exercised via real insertion order instead of hand-picked literals.
            CategoryRule ruleA = baseRuleBuilder().keywords(List.of("AAAA")).priority(5).build();
            CategoryRule ruleB = baseRuleBuilder().keywords(List.of("ZZZZ")).priority(5).build();
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

        @Test
        @DisplayName("PF-315: a multi-keyword rule with a longer combined keyword length is treated "
                + "as more specific (sorted before) a same-priority single-keyword rule")
        void shouldTreatMultiKeywordRuleAsMoreSpecific() {
            // arrange -- both priority 5; "A" (1 char) alone vs "BBBBB"+"CCCCC" (10 chars combined)
            CategoryRule shortSingle = baseRuleBuilder().keywords(List.of("A")).priority(5).build();
            CategoryRule longMulti = baseRuleBuilder().keywords(List.of("BBBBB", "CCCCC")).priority(5).build();
            repository.insertAndReturnId(shortSingle);
            Long longMultiId = repository.insertAndReturnId(longMulti);

            // act
            List<CategoryRule> result = repository.findByUserId(USER_1);

            // assert & verify -- the multi-keyword rule sorts first among the two new ones despite
            // being inserted second, since specificity (not insertion order) is the tie-breaker here
            List<CategoryRule> newRules = result.stream()
                    .filter(r -> r.getKeywords().equals(List.of("A")) || r.getId().equals(longMultiId))
                    .toList();
            assertEquals(2, newRules.size());
            assertEquals(longMultiId, newRules.get(0).getId());
        }

        @Test
        @DisplayName("PF-315: findByUserId assembles a multi-keyword rule's full keyword set in "
                + "insertion order")
        void shouldAssembleMultiKeywordSetOnFind() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("AMZN", "MKTP")).matchType(MatchType.AND).build();
            repository.insertAndReturnId(rule);

            // act
            List<CategoryRule> result = repository.findByUserId(USER_1);

            // assert & verify
            CategoryRule found = result.stream()
                    .filter(r -> r.getKeywords().contains("AMZN"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(List.of("AMZN", "MKTP"), found.getKeywords());
            assertEquals(MatchType.AND, found.getMatchType());
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
            CategoryRule rule = baseRuleBuilder()
                    .keywords(List.of("TEST"))
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();

            // act
            Long generatedId = repository.insertAndReturnId(rule);

            // assert & verify
            assertNotNull(generatedId);
            assertTrue(generatedId > 0);
            CategoryRule persisted = repository.findById(generatedId).orElseThrow();
            assertEquals(List.of("TEST"), persisted.getKeywords());
            assertEquals(1, persisted.getPriority());
            assertEquals(new java.math.BigDecimal("5.00"), persisted.getMinAmount());
            assertEquals(new java.math.BigDecimal("20.00"), persisted.getMaxAmount());
        }

        @Test
        @DisplayName("PF-315: should insert all of a rule's keywords, not just the first")
        void shouldInsertAllKeywords() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("AMZN", "MKTP", "US")).matchType(MatchType.AND).build();

            // act
            Long generatedId = repository.insertAndReturnId(rule);

            // assert & verify
            CategoryRule persisted = repository.findById(generatedId).orElseThrow();
            assertEquals(List.of("AMZN", "MKTP", "US"), persisted.getKeywords());
            assertEquals(MatchType.AND, persisted.getMatchType());
        }

        @Test
        @DisplayName("PF-314: should actually persist an update (previously threw "
                + "UnsupportedOperationException on every call -- the repository never overrode "
                + "the interface's default, unconditionally-throwing update())")
        void shouldUpdate() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("ORIGINAL")).build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updated = rule.toBuilder()
                    .id(id)
                    .keywords(List.of("UPDATED"))
                    .priority(99)
                    .minAmount(new java.math.BigDecimal("10.00"))
                    .maxAmount(new java.math.BigDecimal("50.00"))
                    .build();

            // act
            int rows = repository.update(updated);

            // assert & verify
            assertEquals(1, rows);
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of("UPDATED"), persisted.getKeywords());
            assertEquals(99, persisted.getPriority());
            assertEquals(new java.math.BigDecimal("10.00"), persisted.getMinAmount());
            assertEquals(new java.math.BigDecimal("50.00"), persisted.getMaxAmount());
        }

        @Test
        @DisplayName("PF-315: update should wholesale replace the keyword set -- growing from one "
                + "keyword to several, and changing matchType, both take effect")
        void shouldReplaceKeywordSetOnUpdate() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("OLD")).matchType(MatchType.OR).build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updated = rule.toBuilder()
                    .id(id)
                    .keywords(List.of("NEW1", "NEW2"))
                    .matchType(MatchType.AND)
                    .build();

            // act
            repository.update(updated);

            // assert & verify -- old keyword is gone, both new ones present, matchType changed
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of("NEW1", "NEW2"), persisted.getKeywords());
            assertEquals(MatchType.AND, persisted.getMatchType());
        }

        @Test
        @DisplayName("PF-314: update should affect zero rows when the userId doesn't match -- the "
                + "SQL-level ownership scope (defense-in-depth's last layer, matching the pattern "
                + "already established for accounts and merchants), not just the Controller's "
                + "@PreAuthorize or the Service's own findById+equals check")
        void shouldNotUpdateWhenUserIdDoesNotMatch() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("ORIGINAL")).build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updateAttempt = rule.toBuilder()
                    .id(id)
                    .keywords(List.of("SHOULD_NOT_APPLY"))
                    .user(User.builder().id(999L).build()) // wrong user
                    .build();

            // act
            int rows = repository.update(updateAttempt);

            // assert & verify -- no rows affected, original data untouched
            assertEquals(0, rows);
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of("ORIGINAL"), persisted.getKeywords());
        }

        @Test
        @DisplayName("PF-315: a rejected (wrong-userId) update must not touch the rule's keywords "
                + "either -- the repository only replaces keywords when the parent row's own "
                + "ownership-scoped update actually affected a row")
        void shouldNotReplaceKeywordsWhenUserIdDoesNotMatch() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("A", "B")).build();
            Long id = repository.insertAndReturnId(rule);
            CategoryRule updateAttempt = rule.toBuilder()
                    .id(id)
                    .keywords(List.of("SHOULD_NOT_APPLY"))
                    .user(User.builder().id(999L).build())
                    .build();

            // act
            repository.update(updateAttempt);

            // assert & verify -- the original two-keyword set is fully intact
            CategoryRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of("A", "B"), persisted.getKeywords());
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
        @DisplayName("PF-315: deleting a rule also removes its keyword rows (ON DELETE CASCADE), "
                + "not just the parent row")
        void shouldCascadeDeleteKeywords() {
            // arrange
            CategoryRule rule = baseRuleBuilder().keywords(List.of("TEMP")).build();
            Long id = repository.insertAndReturnId(rule);

            // act
            repository.deleteById(id, USER_1);

            // assert & verify -- the rule (and implicitly its keyword rows, or findById would
            // throw trying to fetch keywords for a rule row that no longer resolves) is gone
            assertTrue(repository.findById(id).isEmpty());
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for insecure deleteById")
        void shouldThrowOnInsecureDelete() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }
    }
}
