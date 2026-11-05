package com.mayureshpatel.pfdataservice.repository.merchant;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.Merchant;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.BaseRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@code MerchantRuleRepository} against a real PostgreSQL instance: the keyword child-table mapping, priority and specificity ordering, ownership-scoped writes and the ON DELETE CASCADE from merchants and rules. */
@Import(MerchantRuleRepository.class)
@DisplayName("MerchantRuleRepository Integration Tests (PostgreSQL)")
class MerchantRuleRepositoryTest extends BaseRepositoryTest {

    private static final Long USER_1 = 1L;
    private static final Long WHOLE_FOODS_ID = 1L;
    private static final Long AMAZON_ID = 2L;
    private static final String KEYWORD_KROGER = "KROGER";
    private static final String KEYWORD_ORIGINAL = "ORIGINAL";

    @Autowired
    private MerchantRuleRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private MerchantRule.MerchantRuleBuilder baseRule() {
        return MerchantRule.builder()
                .priority(1)
                .matchType(MatchType.OR)
                .merchant(Merchant.builder().id(WHOLE_FOODS_ID).build())
                .user(User.builder().id(USER_1).build());
    }

    private long countKeywordRows(Long ruleId) {
        return jdbcClient.sql("select count(*) from merchant_rule_keywords where rule_id = :id")
                .param("id", ruleId).query(Long.class).single();
    }

    /** {@code findByUserId} orders by priority desc, summed keyword length desc, then id asc; results carry their merchant and keyword set; {@code findById} returns one rule or empty. */
    @Nested
    @DisplayName("Find Operations")
    class FindTests {
        @Test
        @DisplayName("should order by priority desc, then summed keyword length desc, then id asc")
        void shouldOrderBySqlRules() {
            // arrange
            Long lowPriority = repository.insertAndReturnId(baseRule().keywords(List.of("ZZZZZZZZZZ")).priority(1).build());
            Long shortKeyword = repository.insertAndReturnId(baseRule().keywords(List.of("A")).priority(5).build());
            Long longMulti = repository.insertAndReturnId(baseRule().keywords(List.of("BBBBB", "CCCCC")).priority(5).build());
            Long tieFirst = repository.insertAndReturnId(baseRule().keywords(List.of("DDDD")).priority(9).build());
            Long tieSecond = repository.insertAndReturnId(baseRule().keywords(List.of("EEEE")).priority(9).build());

            // act
            List<MerchantRule> result = repository.findByUserId(USER_1);

            // assert & verify
            assertEquals(List.of(tieFirst, tieSecond, longMulti, shortKeyword, lowPriority),
                    result.stream().map(MerchantRule::getId).toList());
        }

        @Test
        @DisplayName("should attach the merchant, owner and full keyword set in insertion order")
        void shouldAssembleRule() {
            // arrange
            Long id = repository.insertAndReturnId(baseRule()
                    .merchant(Merchant.builder().id(AMAZON_ID).build())
                    .keywords(List.of(KEYWORD_KROGER, "MKTP")).matchType(MatchType.AND).build());

            // act
            MerchantRule found = repository.findByUserId(USER_1).stream()
                    .filter(r -> r.getId().equals(id)).findFirst().orElseThrow();

            // assert & verify
            assertEquals(List.of(KEYWORD_KROGER, "MKTP"), found.getKeywords());
            assertEquals(MatchType.AND, found.getMatchType());
            assertEquals(USER_1, found.getUser().getId());
            assertEquals(AMAZON_ID, found.getMerchant().getId());
            assertEquals("Amazon", found.getMerchant().getName());
        }

        @Test
        @DisplayName("should not return another user's rules")
        void shouldScopeToUser() {
            // arrange
            repository.insertAndReturnId(baseRule().keywords(List.of(KEYWORD_KROGER)).build());

            // act
            List<MerchantRule> result = repository.findByUserId(2L);

            // assert & verify
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findById should return the rule with its keywords, or empty when missing")
        void shouldFindById() {
            // arrange
            Long id = repository.insertAndReturnId(baseRule().keywords(List.of(KEYWORD_KROGER)).build());

            // act & assert & verify
            MerchantRule found = repository.findById(id).orElseThrow();
            assertEquals(List.of(KEYWORD_KROGER), found.getKeywords());
            assertEquals(WHOLE_FOODS_ID, found.getMerchant().getId());
            assertTrue(repository.findById(999_999L).isEmpty());
        }
    }

    /** {@code insertAndReturnId}, {@code update} and {@code deleteById} persist correctly, replace keywords wholesale, are scoped to the owning user, and cascade. */
    @Nested
    @DisplayName("Write Operations")
    class WriteTests {
        @Test
        @DisplayName("should insert a rule, generate an id and round-trip the amount range")
        void shouldInsert() {
            // arrange
            MerchantRule rule = baseRule().keywords(List.of(KEYWORD_KROGER, "FUEL"))
                    .minAmount(new BigDecimal("5.00")).maxAmount(new BigDecimal("20.00")).build();

            // act
            Long id = repository.insertAndReturnId(rule);

            // assert & verify
            assertNotNull(id);
            MerchantRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of(KEYWORD_KROGER, "FUEL"), persisted.getKeywords());
            assertEquals(1, persisted.getPriority());
            assertEquals(new BigDecimal("5.00"), persisted.getMinAmount());
            assertEquals(new BigDecimal("20.00"), persisted.getMaxAmount());
        }

        @Test
        @DisplayName("should store null amount bounds as null")
        void shouldStoreNullAmounts() {
            // act
            Long id = repository.insertAndReturnId(baseRule().keywords(List.of(KEYWORD_KROGER)).build());

            // assert & verify
            MerchantRule persisted = repository.findById(id).orElseThrow();
            assertEquals(null, persisted.getMinAmount());
            assertEquals(null, persisted.getMaxAmount());
        }

        @Test
        @DisplayName("update should replace fields, merchant and the whole keyword set")
        void shouldUpdate() {
            // arrange
            MerchantRule rule = baseRule().keywords(List.of("OLD")).matchType(MatchType.OR).build();
            Long id = repository.insertAndReturnId(rule);
            MerchantRule updated = rule.toBuilder()
                    .id(id)
                    .merchant(Merchant.builder().id(AMAZON_ID).build())
                    .keywords(List.of("NEW1", "NEW2"))
                    .matchType(MatchType.AND)
                    .priority(99)
                    .minAmount(new BigDecimal("10.00"))
                    .maxAmount(new BigDecimal("50.00"))
                    .build();

            // act
            int rows = repository.update(updated);

            // assert & verify
            assertEquals(1, rows);
            MerchantRule persisted = repository.findById(id).orElseThrow();
            assertEquals(List.of("NEW1", "NEW2"), persisted.getKeywords());
            assertEquals(MatchType.AND, persisted.getMatchType());
            assertEquals(AMAZON_ID, persisted.getMerchant().getId());
            assertEquals(99, persisted.getPriority());
            assertEquals(new BigDecimal("10.00"), persisted.getMinAmount());
        }

        @Test
        @DisplayName("update by the wrong user should affect zero rows and leave the keywords intact")
        void shouldNotUpdateForWrongUser() {
            // arrange
            MerchantRule rule = baseRule().keywords(List.of(KEYWORD_ORIGINAL, "B")).build();
            Long id = repository.insertAndReturnId(rule);
            MerchantRule attempt = rule.toBuilder()
                    .id(id)
                    .keywords(List.of("SHOULD_NOT_APPLY"))
                    .user(User.builder().id(2L).build())
                    .build();

            // act
            int rows = repository.update(attempt);

            // assert & verify
            assertEquals(0, rows);
            assertEquals(List.of(KEYWORD_ORIGINAL, "B"), repository.findById(id).orElseThrow().getKeywords());
        }

        @Test
        @DisplayName("deleteById should delete the rule and its keyword rows")
        void shouldDeleteWithKeywords() {
            // arrange
            Long id = repository.insertAndReturnId(baseRule().keywords(List.of("A", "B")).build());

            // act
            int rows = repository.deleteById(id, USER_1);

            // assert & verify
            assertEquals(1, rows);
            assertTrue(repository.findById(id).isEmpty());
            assertEquals(0, countKeywordRows(id));
        }

        @Test
        @DisplayName("deleteById by the wrong user should delete nothing")
        void shouldNotDeleteForWrongUser() {
            // arrange
            Long id = repository.insertAndReturnId(baseRule().keywords(List.of(KEYWORD_ORIGINAL)).build());

            // act
            int rows = repository.deleteById(id, 2L);

            // assert & verify
            assertEquals(0, rows);
            assertTrue(repository.findById(id).isPresent());
        }

        @Test
        @DisplayName("deleting a merchant should delete its rules and their keywords")
        void shouldCascadeOnMerchantDelete() {
            // arrange
            Long onWholeFoods = repository.insertAndReturnId(baseRule().keywords(List.of("WFM", "WHOLE")).build());
            Long onAmazon = repository.insertAndReturnId(baseRule()
                    .merchant(Merchant.builder().id(AMAZON_ID).build()).keywords(List.of("AMZN")).build());

            // act
            jdbcClient.sql("delete from merchants where id = :id").param("id", WHOLE_FOODS_ID).update();

            // assert & verify
            assertTrue(repository.findById(onWholeFoods).isEmpty());
            assertEquals(0, countKeywordRows(onWholeFoods));
            assertTrue(repository.findById(onAmazon).isPresent());
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for insecure deleteById")
        void shouldThrowOnInsecureDelete() {
            assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
        }
    }
}
