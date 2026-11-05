package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@code RuleMatcher} and {@code RuleOrdering} work on any {@link MatchableRule}, not just a
 * category rule, so a second kind of rule (PF-EPIC-049) matches and ranks exactly as category rules do.
 */
@DisplayName("MatchableRule sharing Unit Tests")
class MatchableRuleSharingTest {

    private static final String KEYWORD = "kroger";

    private final RuleMatcher matcher = new RuleMatcher();

    /** A minimal rule that is not a category rule. */
    private record OtherRule(Long id, List<String> keywords, MatchType matchType, Integer priority,
                             BigDecimal minAmount, BigDecimal maxAmount) implements MatchableRule {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public List<String> getKeywords() {
            return keywords;
        }

        @Override
        public MatchType getMatchType() {
            return matchType;
        }

        @Override
        public Integer getPriority() {
            return priority;
        }

        @Override
        public BigDecimal getMinAmount() {
            return minAmount;
        }

        @Override
        public BigDecimal getMaxAmount() {
            return maxAmount;
        }
    }

    private static OtherRule rule(Long id, String keyword, int priority) {
        return new OtherRule(id, List.of(keyword), MatchType.OR, priority, null, null);
    }

    /** The matcher applies keyword and amount-range semantics to a non-category rule. */
    @Test
    @DisplayName("matches_nonCategoryRule_appliesKeywordAndRange")
    void matches_nonCategoryRule_appliesKeywordAndRange() {
        // arrange
        OtherRule rule = new OtherRule(1L, List.of(KEYWORD), MatchType.OR, 0, new BigDecimal("5.00"), new BigDecimal("50.00"));

        // act & assert & verify
        assertTrue(matcher.matches("KROGER #340", new BigDecimal("-20.00"), rule));
        assertFalse(matcher.matches("KROGER #340", new BigDecimal("-60.00"), rule));
        assertFalse(matcher.matches("TARGET", new BigDecimal("-20.00"), rule));
    }

    /** The ordering ranks a non-category draft by priority, then specificity, then id. */
    @Test
    @DisplayName("rank_nonCategoryRules_ordersByPriorityThenLengthThenId")
    void rank_nonCategoryRules_ordersByPriorityThenLengthThenId() {
        // arrange
        OtherRule low = rule(1L, KEYWORD, 1);
        OtherRule high = rule(2L, "k", 9);
        OtherRule draft = rule(Long.MAX_VALUE, KEYWORD, 1);

        // act
        List<OtherRule> ranked = RuleOrdering.rank(List.of(low, high), draft, null);

        // assert & verify
        assertEquals(List.of(2L, 1L, Long.MAX_VALUE), ranked.stream().map(OtherRule::getId).toList());
    }

    /** An edited draft replaces its saved version instead of competing with it. */
    @Test
    @DisplayName("rank_nonCategoryRules_editedDraftReplacesSavedRule")
    void rank_nonCategoryRules_editedDraftReplacesSavedRule() {
        // arrange
        OtherRule saved = rule(7L, KEYWORD, 1);
        OtherRule draft = rule(7L, KEYWORD, 5);

        // act
        List<OtherRule> ranked = RuleOrdering.rank(List.of(saved), draft, 7L);

        // assert & verify
        assertEquals(1, ranked.size());
        assertEquals(5, ranked.get(0).getPriority());
    }
}
