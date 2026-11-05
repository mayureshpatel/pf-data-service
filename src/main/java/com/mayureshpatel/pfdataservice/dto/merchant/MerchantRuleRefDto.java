package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * A compact reference to a saved merchant rule (PF-905), used by the match preview to name the
 * other rules that also match a transaction.
 *
 * @param id           the rule id
 * @param keywords     the rule's keyword set
 * @param merchantName the name of the merchant the rule points at, may be null
 * @param priority     the rule's priority (higher wins)
 */
public record MerchantRuleRefDto(
        Long id,
        List<String> keywords,
        String merchantName,
        Integer priority
) {
}
