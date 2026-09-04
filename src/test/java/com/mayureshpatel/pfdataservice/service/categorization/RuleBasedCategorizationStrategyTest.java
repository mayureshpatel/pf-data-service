package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.dto.transaction.TransactionUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuleBasedCategorizationStrategy Unit Tests")
class RuleBasedCategorizationStrategyTest {

    private final RuleBasedCategorizationStrategy strategy = new RuleBasedCategorizationStrategy();

    @Nested
    @DisplayName("categorize (Domain)")
    class CategorizeDomainTests {
        @Test
        @DisplayName("should return category ID if keyword matches description case-insensitively")
        void shouldMatchKeyword() {
            // Arrange
            Transaction t = Transaction.builder().description("AMAZON MARKETPLACE").build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .build();

            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(t, context);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(10L, result.get());
        }

        @Test
        @DisplayName("should return empty if no rules match")
        void shouldNotMatch() {
            // Arrange
            Transaction t = Transaction.builder().description("Unknown").build();
            CategoryRule rule = CategoryRule.builder().keyword("Amazon").build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(t, context);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty if description or rules are missing")
        void shouldHandleNulls() {
            // Act & Assert
            assertTrue(strategy.categorize(Transaction.builder().description(null).build(), CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build()).isEmpty());
            assertTrue(strategy.categorize(Transaction.builder().description("Test").build(), CategorizationStrategy.CategorizationContext.builder().rules(null).build()).isEmpty());
        }

        @Test
        @DisplayName("should handle empty rules list")
        void shouldHandleEmptyRules() {
            // Act
            Optional<Long> result = strategy.categorize(Transaction.builder().description("Test").build(),
                    CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build());

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("PF-313: when two rules both match, the FIRST one in the rules list wins -- this "
                + "strategy trusts list order as priority order rather than re-sorting itself, since "
                + "CategoryRuleQueries.FIND_ALL_BY_USER_ID already delivers rules in priority order")
        void shouldTakeFirstMatchingRuleWhenMultipleMatch() {
            // arrange -- both rules match "WHOLEFDS #1234"; higher-priority rule listed first,
            // matching how the real query already orders them (priority desc)
            Transaction t = Transaction.builder().description("WHOLEFDS #1234").build();
            CategoryRule higherPriorityRule = CategoryRule.builder()
                    .keyword("WHOLEFDS")
                    .category(Category.builder().id(7L).build()) // Groceries
                    .priority(10)
                    .build();
            CategoryRule lowerPriorityRule = CategoryRule.builder()
                    .keyword("FDS")
                    .category(Category.builder().id(99L).build()) // some unrelated category
                    .priority(1)
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(higherPriorityRule, lowerPriorityRule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(7L, result.get());
        }

        @Test
        @DisplayName("PF-313: genuinely list-order-dependent, not coincidence -- reversing the same "
                + "two rules flips which category wins")
        void shouldFollowListOrderNotSomeOtherImplicitRule() {
            // arrange -- identical rules to the previous test, but reversed
            Transaction t = Transaction.builder().description("WHOLEFDS #1234").build();
            CategoryRule higherPriorityRule = CategoryRule.builder()
                    .keyword("WHOLEFDS")
                    .category(Category.builder().id(7L).build())
                    .priority(10)
                    .build();
            CategoryRule lowerPriorityRule = CategoryRule.builder()
                    .keyword("FDS")
                    .category(Category.builder().id(99L).build())
                    .priority(1)
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(lowerPriorityRule, higherPriorityRule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isPresent());
            assertEquals(99L, result.get());
        }

        @Test
        @DisplayName("PF-314: the ticket's own worked example -- \"Amazon\" under $20 goes to "
                + "Household, over $100 goes to Electronics")
        void shouldPickTheRangeThatMatchesTheTransactionAmount() {
            // arrange
            CategoryRule underTwenty = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(1L).build()) // Household
                    .priority(0)
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();
            CategoryRule overOneHundred = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(2L).build()) // Electronics
                    .priority(0)
                    .minAmount(new java.math.BigDecimal("100.00"))
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(underTwenty, overOneHundred))
                    .build();

            // act
            Optional<Long> cheapResult = strategy.categorize(
                    Transaction.builder().description("AMAZON MKTP").amount(new java.math.BigDecimal("15.00")).build(), context);
            Optional<Long> expensiveResult = strategy.categorize(
                    Transaction.builder().description("AMAZON MKTP").amount(new java.math.BigDecimal("150.00")).build(), context);
            Optional<Long> midRangeResult = strategy.categorize(
                    Transaction.builder().description("AMAZON MKTP").amount(new java.math.BigDecimal("50.00")).build(), context);

            // assert & verify
            assertEquals(1L, cheapResult.orElseThrow());
            assertEquals(2L, expensiveResult.orElseThrow());
            assertTrue(midRangeResult.isEmpty(), "an amount matching neither range should not match either rule");
        }

        @Test
        @DisplayName("PF-314: should match when the amount falls exactly on the min boundary "
                + "(inclusive)")
        void shouldMatchAtMinBoundaryInclusive() {
            // arrange
            Transaction t = Transaction.builder().description("Amazon").amount(new java.math.BigDecimal("5.00")).build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertEquals(10L, result.orElseThrow());
        }

        @Test
        @DisplayName("PF-314: should match when the amount falls exactly on the max boundary "
                + "(inclusive)")
        void shouldMatchAtMaxBoundaryInclusive() {
            // arrange
            Transaction t = Transaction.builder().description("Amazon").amount(new java.math.BigDecimal("20.00")).build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertEquals(10L, result.orElseThrow());
        }

        @Test
        @DisplayName("PF-314: keyword match alone remains sufficient when a rule has no range set")
        void shouldMatchOnKeywordAloneWhenNoRangeSet() {
            // arrange -- amount is wildly large, but the rule sets no range at all
            Transaction t = Transaction.builder().description("Amazon").amount(new java.math.BigDecimal("99999.00")).build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertEquals(10L, result.orElseThrow());
        }

        @Test
        @DisplayName("PF-314: a rule with a range set should not match when the transaction's amount "
                + "is unknown (null) -- there's nothing to compare the range against")
        void shouldNotMatchRangeRuleWhenTransactionAmountIsNull() {
            // arrange
            Transaction t = Transaction.builder().description("Amazon").amount(null).build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(10L).build())
                    .minAmount(new java.math.BigDecimal("5.00"))
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(t, context);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("categorize (Request)")
    class CategorizeRequestTests {
        @Test
        @DisplayName("should match keyword for update request")
        void shouldMatchRequest() {
            // Arrange
            TransactionUpdateRequest req = TransactionUpdateRequest.builder().description("Netflix.com").build();
            CategoryRule rule = CategoryRule.builder()
                    .keyword("Netflix")
                    .category(Category.builder().id(20L).build())
                    .build();

            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(rule))
                    .build();

            // Act
            Optional<Long> result = strategy.categorize(req, context);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(20L, result.get());
        }

        @Test
        @DisplayName("should return empty if description or rules are missing for request")
        void shouldHandleNullsInRequest() {
            // Act & Assert
            assertTrue(strategy.categorize(TransactionUpdateRequest.builder().description(null).build(), CategorizationStrategy.CategorizationContext.builder().rules(List.of()).build()).isEmpty());
            assertTrue(strategy.categorize(TransactionUpdateRequest.builder().description("Test").build(), CategorizationStrategy.CategorizationContext.builder().rules(null).build()).isEmpty());
        }

        @Test
        @DisplayName("PF-314: amount-range check also applies to the edited-transaction (request) path")
        void shouldRespectAmountRangeForRequest() {
            // arrange
            TransactionUpdateRequest req = TransactionUpdateRequest.builder()
                    .description("Amazon")
                    .amount(new java.math.BigDecimal("500.00"))
                    .build();
            CategoryRule tooNarrow = CategoryRule.builder()
                    .keyword("Amazon")
                    .category(Category.builder().id(20L).build())
                    .maxAmount(new java.math.BigDecimal("20.00"))
                    .build();
            CategorizationStrategy.CategorizationContext context = CategorizationStrategy.CategorizationContext.builder()
                    .rules(List.of(tooNarrow))
                    .build();

            // act
            Optional<Long> result = strategy.categorize(req, context);

            // assert & verify
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void shouldReturnOrder() {
        assertEquals(100, strategy.getOrder());
    }
}
