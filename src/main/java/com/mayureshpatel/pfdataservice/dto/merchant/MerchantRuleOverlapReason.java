package com.mayureshpatel.pfdataservice.dto.merchant;

/**
 * Why the winner of an overlap wins, in the terms import uses: an exact description link beats any
 * rule, then the rule order (priority, then summed keyword length, then id).
 */
public enum MerchantRuleOverlapReason {
    /** the description has an exact description link to the other merchant */
    LINK,
    /** the winning rule has the higher priority */
    HIGHER_PRIORITY,
    /** same priority; the winning rule's keywords are longer in total */
    LONGER_KEYWORDS,
    /** same priority and keyword length; the winning rule has the lower id (it was saved first) */
    LOWER_ID
}
