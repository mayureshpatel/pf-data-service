package com.mayureshpatel.pfdataservice.service.categorization;

import com.mayureshpatel.pfdataservice.domain.category.CategoryRule;
import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Decides whether a single {@link CategoryRule} matches a description and amount: the rule's
 * keywords must match the description (PF-315) and the amount must fall within the rule's
 * optional range (PF-314). Shared by {@link RuleBasedCategorizationStrategy} (import) and the
 * rule match preview so both apply identical semantics.
 */
@Component
public class RuleMatcher {

    /**
     * Checks a rule against a description and amount.
     *
     * @param description the transaction description, may be null (never matches)
     * @param amount      the transaction amount, may be null
     * @param rule        the rule to check
     * @return true if both the keyword match and the amount range permit a match
     */
    public boolean matches(String description, BigDecimal amount, CategoryRule rule) {
        return description != null
                && matchesKeywords(description, rule)
                && matchesAmountRange(amount, rule);
    }

    /**
     * Checks a rule's keyword set against a description (PF-315), case-insensitively, combining
     * per the rule's {@link MatchType}. A rule with no keywords never matches -- deliberately not
     * left to fall out of {@code allMatch}'s vacuous truth on an empty stream, which would
     * otherwise make an empty-keyword {@code AND} rule match every transaction.
     *
     * @param description the transaction description to match against
     * @param rule        the rule whose keyword set to check
     * @return true if the rule's keyword set matches the description
     */
    private boolean matchesKeywords(String description, CategoryRule rule) {
        if (rule.getKeywords() == null || rule.getKeywords().isEmpty()) {
            return false;
        }

        String lowerDescription = description.toLowerCase(Locale.ROOT);
        if (rule.getMatchType() == MatchType.AND) {
            return rule.getKeywords().stream().allMatch(keyword -> lowerDescription.contains(keyword.toLowerCase(Locale.ROOT)));
        }
        return rule.getKeywords().stream().anyMatch(keyword -> lowerDescription.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    /**
     * Checks a rule's optional amount range (PF-314) against a transaction's absolute amount. A
     * rule with neither bound set always passes -- keyword match alone remains sufficient. A rule
     * with a range set but an unknown (null) transaction amount never passes, since there's nothing
     * to compare. Compares the amount's magnitude rather than its raw signed value, matching how a
     * range like "under $20" or "over $100" is naturally understood regardless of expense/income
     * sign.
     *
     * @param amount the transaction's amount, may be null
     * @param rule   the rule whose range to check
     * @return true if the rule's range (if any) permits a match
     */
    private boolean matchesAmountRange(BigDecimal amount, CategoryRule rule) {
        if (rule.getMinAmount() == null && rule.getMaxAmount() == null) {
            return true;
        }
        if (amount == null) {
            return false;
        }

        BigDecimal magnitude = amount.abs();
        if (rule.getMinAmount() != null && magnitude.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        return rule.getMaxAmount() == null || magnitude.compareTo(rule.getMaxAmount()) <= 0;
    }
}
