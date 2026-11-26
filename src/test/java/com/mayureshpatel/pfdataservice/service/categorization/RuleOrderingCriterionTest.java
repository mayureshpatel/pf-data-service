package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import com.mayureshpatel.pfdataservice.domain.merchant.MerchantRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link RuleOrdering#decidingCriterion} names the first sort key that separates two
 * rules (priority, then summed keyword length, then id), the reason PF-981's overlaps report.
 */
@DisplayName("RuleOrdering deciding criterion")
class RuleOrderingCriterionTest {

    private static MerchantRule rule(long id, Integer priority, String... keywords) {
        return MerchantRule.builder().id(id).priority(priority).matchType(MatchType.OR).keywords(List.of(keywords)).build();
    }

    @Test
    @DisplayName("should name priority when priorities differ, even with longer keywords behind")
    void shouldNamePriority() {
        // arrange
        MerchantRule ahead = rule(9L, 5, "a");
        MerchantRule behind = rule(1L, 0, "much longer keyword");

        // act
        RuleOrdering.Criterion criterion = RuleOrdering.decidingCriterion(ahead, behind);

        // assert & verify
        assertThat(criterion).isEqualTo(RuleOrdering.Criterion.PRIORITY);
        assertThat(RuleOrdering.compare(ahead, behind)).isNegative();
    }

    @Test
    @DisplayName("should name keyword length on a priority tie, and id on a full tie")
    void shouldNameKeywordLengthThenId() {
        // arrange
        MerchantRule longer = rule(9L, 0, "kroger fuel");
        MerchantRule shorter = rule(1L, 0, "kroger");
        MerchantRule twin = rule(2L, 0, "KROGER");

        // act
        RuleOrdering.Criterion byLength = RuleOrdering.decidingCriterion(longer, shorter);
        RuleOrdering.Criterion byId = RuleOrdering.decidingCriterion(shorter, twin);

        // assert & verify
        assertThat(byLength).isEqualTo(RuleOrdering.Criterion.KEYWORD_LENGTH);
        assertThat(byId).isEqualTo(RuleOrdering.Criterion.ID);
        assertThat(RuleOrdering.compare(shorter, twin)).isNegative();
    }

    @Test
    @DisplayName("should treat a null priority as different from zero")
    void shouldSeparateNullPriority() {
        // arrange
        MerchantRule nullPriority = rule(9L, null, "a");
        MerchantRule zero = rule(1L, 0, "a");

        // act
        RuleOrdering.Criterion criterion = RuleOrdering.decidingCriterion(nullPriority, zero);

        // assert & verify
        assertThat(criterion).isEqualTo(RuleOrdering.Criterion.PRIORITY);
    }
}
