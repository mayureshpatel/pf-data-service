package com.mayureshpatel.pfdataservice.dto.merchant;

/**
 * The PF-905 match-preview verdict for one transaction a draft merchant rule matches. Only a
 * transaction with no merchant and no exact description link can ever be changed by a merchant
 * rule, so only those get a win/lose verdict; the rest are reported for information only. The
 * declaration order is the row display order.
 */
public enum MerchantRuleVerdict {
    /** no merchant, no exact link, and the draft is the first rule in the effective order that matches it. */
    WILL_ASSIGN,
    /** no merchant and no exact link, but a saved rule ranked ahead of the draft matches it first. */
    LOSES_TO_OTHER_RULE,
    /** already has a merchant, so the rule leaves it alone whatever the draft says. */
    ALREADY_HAS_MERCHANT,
    /** has no merchant but its description has an exact link, and an exact link always beats a rule. */
    LINKED
}
