package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleBasedCategorizationStrategy unit tests")
class RuleBasedCategorizationStrategyTest {

    private final RuleBasedCategorizationStrategy strategy = new RuleBasedCategorizationStrategy();

    private Transaction buildTransaction(String description) {
        return Transaction.builder()
                .description(description)
                .build();
    }

    private CategoryRule buildRule(String keyword, Long categoryId) {
        return CategoryRule.builder()
                .keyword(keyword)
                .category(
                        Category.builder()
                                .id(categoryId)
                                .build()
                )
                .build();
    }

    private CategorizationStrategy.CategorizationContext buildContext(List<CategoryRule> rules) {
        return CategorizationStrategy.CategorizationContext.builder()
                .rules(rules)
                .build();
    }

    @Nested
    @DisplayName("categorize() — guard conditions")
    class GuardConditionTests {

        @Test
        @DisplayName("should return empty when transaction description is null")
        void categorize_nullDescription_returnsEmpty() {
            Transaction t = buildTransaction(null);
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("coffee", 1L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when context rules are null")
        void categorize_nullRules_returnsEmpty() {
            Transaction t = buildTransaction("Starbucks coffee");
            CategorizationStrategy.CategorizationContext ctx = buildContext(null);

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("categorize() — matching")
    class MatchingTests {

        @Test
        @DisplayName("should return category ID when keyword is found in description")
        void categorize_keywordFoundInDescription_returnsCategoryId() {
            Transaction t = buildTransaction("Starbucks coffee shop");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("coffee", 10L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(10L);
        }

        @Test
        @DisplayName("should return empty when no keyword matches the description")
        void categorize_noMatchingKeyword_returnsEmpty() {
            Transaction t = buildTransaction("Amazon purchase");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("grocery", 5L), buildRule("restaurant", 6L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should match when description is uppercase and keyword is lowercase")
        void categorize_descriptionUpperCaseKeywordLower_matches() {
            Transaction t = buildTransaction("STARBUCKS COFFEE");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("coffee", 11L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(11L);
        }

        @Test
        @DisplayName("should match when description is lowercase and keyword is uppercase")
        void categorize_descriptionLowerCaseKeywordUpper_matches() {
            Transaction t = buildTransaction("starbucks coffee");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("COFFEE", 12L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(12L);
        }

        @Test
        @DisplayName("should return the first matching rule's category ID when multiple rules match")
        void categorize_multipleMatchingRules_returnsFirstMatch() {
            Transaction t = buildTransaction("Whole Foods Market grocery");
            CategorizationStrategy.CategorizationContext ctx = buildContext(List.of(
                    buildRule("whole foods", 20L),
                    buildRule("grocery", 21L)
            ));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(20L);
        }

        @Test
        @DisplayName("should return empty when rules list is empty")
        void categorize_emptyRulesList_returnsEmpty() {
            Transaction t = buildTransaction("Starbucks");
            CategorizationStrategy.CategorizationContext ctx = buildContext(List.of());

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should match keyword that is a substring of description")
        void categorize_keywordIsSubstringOfDescription_matches() {
            Transaction t = buildTransaction("NETFLIX.COM SUBSCRIPTION");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("netflix", 30L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(30L);
        }

        @Test
        @DisplayName("should match any description when keyword is an empty string")
        void categorize_emptyKeyword_matchesAnyDescription() {
            Transaction t = buildTransaction("Amazon purchase");
            CategorizationStrategy.CategorizationContext ctx = buildContext(
                    List.of(buildRule("", 99L)));

            Optional<Long> result = strategy.categorize(t, ctx);

            assertThat(result).contains(99L);
        }
    }

    @Nested
    @DisplayName("getOrder()")
    class GetOrderTests {

        @Test
        @DisplayName("should return 100")
        void getOrder_returns100() {
            assertThat(strategy.getOrder()).isEqualTo(100);
        }
    }
}
