package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * One group of descriptions where a previewed rule meets another merchant (PF-981): descriptions
 * the rule matches that another merchant takes, or that the rule takes from a rule pointing at
 * another merchant. Grouped by outcome, reason, merchant and rule.
 *
 * @param outcome          whether this rule or the other merchant wins these descriptions
 * @param reason           why the winner wins
 * @param merchantId       the other merchant's id
 * @param merchantName     the other merchant's name
 * @param rule             the other merchant's rule, or {@code null} when it wins through a link
 * @param descriptionCount how many distinct descriptions (case and spacing folded) are in the group
 * @param transactionCount how many of the user's live transactions use them
 * @param descriptions     up to {@value #MAX_DESCRIPTIONS} of the descriptions, most used first
 */
public record MerchantRuleOverlapDto(
        MerchantRuleOverlapOutcome outcome,
        MerchantRuleOverlapReason reason,
        Long merchantId,
        String merchantName,
        MerchantRuleRefDto rule,
        int descriptionCount,
        int transactionCount,
        List<String> descriptions
) {

    /** The most sample descriptions one overlap group lists. */
    public static final int MAX_DESCRIPTIONS = 5;
}
