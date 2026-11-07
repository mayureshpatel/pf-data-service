package com.mayureshpatel.pfdataservice.dto.rule;

import java.util.List;

/**
 * The result of an undo (PF-922).
 *
 * @param restoredCount        how many distinct transactions were put back
 * @param skippedTransactionIds transactions left alone because they changed since the apply, or are not the user's
 * @param categoryRuleDeleted  true if the category rule was found and deleted
 * @param merchantRuleDeleted  true if the merchant rule was found and deleted
 */
public record RuleApplicationUndoResultDto(
        int restoredCount,
        List<Long> skippedTransactionIds,
        boolean categoryRuleDeleted,
        boolean merchantRuleDeleted
) {
}
