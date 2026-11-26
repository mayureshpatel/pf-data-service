package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.rule.MatchableRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private static final Comparator<MatchableRule> ORDER = Comparator
            .comparing(MatchableRule::getPriority, Comparator.nullsFirst(Comparator.<Integer>reverseOrder()))
            .thenComparing(RuleOrdering::keywordLength, Comparator.reverseOrder())
            .thenComparing(MatchableRule::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private RuleOrdering() {
    }

    /**
     * Builds the effective rule order with a draft rule in place: the saved rule with id
     * {@code replacesRuleId} (when editing) is dropped so a rule is never ranked against itself,
     * the draft is added, and the result is sorted the way the SQL sorts. A new draft should carry
     * {@code Long.MAX_VALUE} as its id so it ranks after saved rules it fully ties with, as it
     * would after being saved with a fresh, higher id.
     *
     * @param <R>            the rule type, a category rule or a merchant rule
     * @param existing       the user's saved rules, in any order
     * @param draft          the draft rule to place
     * @param replacesRuleId the id of the saved rule the draft replaces, or null for a new draft
     * @return a new list with every kept saved rule and the draft, in effective order
     */
    public static <R extends MatchableRule> List<R> rank(List<R> existing, R draft, Long replacesRuleId) {
        List<R> ranked = new ArrayList<>(existing.size() + 1);
        for (R rule : existing) {
            if (replacesRuleId == null || !replacesRuleId.equals(rule.getId())) {
                ranked.add(rule);
            }
        }
        ranked.add(draft);
        ranked.sort(ORDER);
        return ranked;
    }

    /** The step of the rule order that decides between two rules, in the order the steps apply. */
    public enum Criterion {
        /** the rules have different priorities; the higher one wins (a null priority counts highest) */
        PRIORITY,
        /** same priority; the rule with the longer summed keyword length wins */
        KEYWORD_LENGTH,
        /** same priority and keyword length; the rule with the lower id wins */
        ID
    }

    /**
     * Says which step of the rule order puts one rule ahead of another, so a caller can explain a
     * winner in the same terms the ordering itself uses (PF-981 overlaps).
     *
     * @param ahead  the rule that ranks first
     * @param behind the rule that ranks after it
     * @return the first step at which the two differ, or {@link Criterion#ID} when only the ids do
     */
    public static Criterion decidingCriterion(MatchableRule ahead, MatchableRule behind) {
        if (!Objects.equals(ahead.getPriority(), behind.getPriority())) {
            return Criterion.PRIORITY;
        }
        if (keywordLength(ahead) != keywordLength(behind)) {
            return Criterion.KEYWORD_LENGTH;
        }
        return Criterion.ID;
    }

    /**
     * Compares two rules in the effective rule order.
     *
     * @param a one rule
     * @param b another rule
     * @return negative when {@code a} ranks first, positive when {@code b} does, zero for a full tie
     */
    public static int compare(MatchableRule a, MatchableRule b) {
        return ORDER.compare(a, b);
    }

    /**
     * The rule's specificity as the SQL computes it: the sum of its keywords' lengths in
     * characters (code points, as postgres {@code length()} counts them, not utf-16 units).
     */
    private static int keywordLength(MatchableRule rule) {
        if (rule.getKeywords() == null) {
            return 0;
        }
        return rule.getKeywords().stream()
                .mapToInt(keyword -> keyword.codePointCount(0, keyword.length()))
                .sum();
    }
}
