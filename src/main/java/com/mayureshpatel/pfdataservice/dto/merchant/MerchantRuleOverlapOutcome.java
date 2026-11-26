package com.mayureshpatel.pfdataservice.dto.merchant;

/** Who takes an overlapping description: the rule being previewed, or another merchant. */
public enum MerchantRuleOverlapOutcome {
    /** the previewed rule wins over a rule that points at another merchant */
    THIS_RULE_WINS,
    /** another merchant takes the description, through a link or a rule ranked ahead */
    OTHER_WINS
}
