package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * The result of creating a merchant rule and applying it to chosen transactions (PF-921).
 *
 * @param ruleId     the new rule's id
 * @param merchantId the merchant the rule set, so an undo can tell a row changed since
 * @param changes  every transaction that was changed, with its previous merchant
 */
public record MerchantRuleApplyResultDto(Long ruleId, Long merchantId, List<AppliedMerchantChangeDto> changes) {
}
