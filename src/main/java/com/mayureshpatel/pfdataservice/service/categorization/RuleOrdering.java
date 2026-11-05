package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reproduces, in Java, the rule order that {@code CategoryRuleQueries.FIND_ALL_BY_USER_ID} gives
 * import and "Apply rules" (PF-313/PF-315): priority descending, then summed keyword length
 * descending, then id ascending. The PF-900 match preview needs this to place an unsaved draft
 * rule among the saved ones exactly where it would land once saved; a test against PostgreSQL
 * ({@code RuleOrderingMatchesSqlOrderingTest}) keeps the two from drifting apart.
 */
public final class RuleOrdering {

    /**
     * Priority descending; a null priority sorts first, as postgres places nulls first in a
     * descending sort.
     */
    private static final Comparator<CategoryRule> ORDER = Comparator
            .comparing(CategoryRule::getPriority, Comparator.nullsFirst(Comparator.<Integer>reverseOrder()))
            .thenComparing(RuleOrdering::keywordLength, Comparator.reverseOrder())
            .thenComparing(CategoryRule::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private RuleOrdering() {
    }

    /**
     * Builds the effective rule order with a draft rule in place: the saved rule with id
     * {@code replacesRuleId} (when editing) is dropped so a rule is never ranked against itself,
     * the draft is added, and the result is sorted the way the SQL sorts. A new draft should carry
     * {@code Long.MAX_VALUE} as its id so it ranks after saved rules it fully ties with, as it
     * would after being saved with a fresh, higher id.
     *
     * @param existing       the user's saved rules, in any order
     * @param draft          the draft rule to place
     * @param replacesRuleId the id of the saved rule the draft replaces, or null for a new draft
     * @return a new list with every kept saved rule and the draft, in effective order
     */
    public static List<CategoryRule> rank(List<CategoryRule> existing, CategoryRule draft, Long replacesRuleId) {
        List<CategoryRule> ranked = new ArrayList<>(existing.size() + 1);
        for (CategoryRule rule : existing) {
            if (replacesRuleId == null || !replacesRuleId.equals(rule.getId())) {
                ranked.add(rule);
            }
        }
        ranked.add(draft);
        ranked.sort(ORDER);
        return ranked;
    }

    /**
     * The rule's specificity as the SQL computes it: the sum of its keywords' lengths in
     * characters (code points, as postgres {@code length()} counts them, not utf-16 units).
     */
    private static int keywordLength(CategoryRule rule) {
        if (rule.getKeywords() == null) {
            return 0;
        }
        return rule.getKeywords().stream()
                .mapToInt(keyword -> keyword.codePointCount(0, keyword.length()))
                .sum();
    }
}
