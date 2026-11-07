package com.mayureshpatel.pfdataservice.dto.rule;

import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyResultDto;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyResultDto;

/**
 * The result of creating and applying a category rule and a merchant rule together (PF-934): each
 * kind's own result, which together are what an undo needs.
 *
 * @param category the category rule's result
 * @param merchant the merchant rule's result
 */
public record CreateBothAndApplyResultDto(
        CategoryRuleApplyResultDto category,
        MerchantRuleApplyResultDto merchant
) {
}
