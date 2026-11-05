package com.mayureshpatel.pfdataservice.domain.rule;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;

import java.math.BigDecimal;
import java.util.List;

/**
 * What every keyword rule shares, whatever it assigns: an id, a keyword set combined per a
 * {@link MatchType}, a priority and an optional amount range. The rule matcher and the rule
 * ordering work on this contract alone, so category rules and merchant rules match and rank
 * identically and cannot drift apart.
 */
public interface MatchableRule {

    /**
     * The rule's id, null for an unsaved draft.
     *
     * @return the id, or null
     */
    Long getId();

    /**
     * The rule's keywords, matched case-insensitively against a description.
     *
     * @return the keywords, may be null or empty (such a rule never matches)
     */
    List<String> getKeywords();

    /**
     * How the keywords combine: {@code AND} requires all, {@code OR} requires at least one.
     *
     * @return the match type
     */
    MatchType getMatchType();

    /**
     * The rule's priority, higher wins; null sorts first as postgres does in a descending sort.
     *
     * @return the priority, or null
     */
    Integer getPriority();

    /**
     * The inclusive lower bound on a transaction's absolute amount.
     *
     * @return the bound, or null when open
     */
    BigDecimal getMinAmount();

    /**
     * The inclusive upper bound on a transaction's absolute amount.
     *
     * @return the bound, or null when open
     */
    BigDecimal getMaxAmount();
}
