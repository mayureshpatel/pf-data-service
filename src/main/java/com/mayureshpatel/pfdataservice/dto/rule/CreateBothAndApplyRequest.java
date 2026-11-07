package com.mayureshpatel.pfdataservice.dto.rule;

import com.mayureshpatel.pfdataservice.dto.category.CategoryRuleApplyRequest;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantRuleApplyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The PF-934 request to create a category rule and a merchant rule from the same keyword and apply
 * both, all or nothing, to the transactions the user chose for each kind.
 *
 * @param category the category rule and the transactions to give its category
 * @param merchant the merchant rule (an existing or a new merchant) and the transactions to give it
 */
public record CreateBothAndApplyRequest(
        @NotNull @Valid CategoryRuleApplyRequest category,
        @NotNull @Valid MerchantRuleApplyRequest merchant
) {
}
