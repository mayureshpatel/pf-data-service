package com.mayureshpatel.pfdataservice.dto.merchant;

import com.mayureshpatel.pfdataservice.domain.category.MatchType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * API view of a merchant rule: its keywords, how they combine, priority, optional amount range
 * and the merchant it points at.
 *
 * @param id        the rule id
 * @param userId    the owning user's id
 * @param keywords  the rule's keywords
 * @param matchType how the keywords combine
 * @param priority  higher wins when several rules match
 * @param merchant  the merchant the rule points at
 * @param minAmount optional lower bound on the absolute amount
 * @param maxAmount optional upper bound on the absolute amount
 */
@Builder
public record MerchantRuleDto(
        Long id,
        Long userId,
        List<String> keywords,
        MatchType matchType,
        Integer priority,
        MerchantDto merchant,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {
}
