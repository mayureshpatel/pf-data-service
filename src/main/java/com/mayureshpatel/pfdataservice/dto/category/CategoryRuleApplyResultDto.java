package com.mayureshpatel.pfdataservice.dto.category;

import java.util.List;

/**
 * The result of creating a category rule and applying it to chosen transactions (PF-920).
 *
 * @param ruleId                 the new rule's id
 * @param categoryId             the category the rule set, so an undo can tell a row changed since
 * @param changes                every transaction that was changed, with its previous category
 * @param skippedTransactionIds  chosen transactions that were left alone because rules never apply
 *                               to their type (transfers)
 */
public record CategoryRuleApplyResultDto(
        Long ruleId,
        Long categoryId,
        List<AppliedCategoryChangeDto> changes,
        List<Long> skippedTransactionIds
) {
}
