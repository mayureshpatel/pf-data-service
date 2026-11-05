package com.mayureshpatel.pfdataservice.dto.category;

/**
 * The PF-900 match-preview verdict for one transaction a draft rule matches. Only uncategorized
 * transactions can ever be changed by "Apply rules" or import, so only those get a win/lose
 * verdict; categorized ones are reported for information only.
 */
public enum RuleVerdict {
    /** uncategorized, and the draft is the first rule in the effective order that matches it. */
    WILL_CATEGORIZE,
    /** uncategorized, but an existing rule ranked ahead of the draft matches it first. */
    LOSES_TO_OTHER_RULE,
    /** already has a category, so applying rules leaves it alone whatever the draft says. */
    ALREADY_CATEGORIZED
}
