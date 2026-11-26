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
 * @param matchedDescriptionCount  how many distinct descriptions (case and spacing folded) of the
 *                                 user's live transactions the rule matches, whether or not it wins
 *                                 them; only filled when the rules are listed for one merchant
 *                                 (PF-981), otherwise {@code null}
 * @param matchedTransactionCount  how many of the user's live transactions it matches, same terms
 */
@Builder(toBuilder = true)
public record MerchantRuleDto(
        Long id,
        Long userId,
        List<String> keywords,
        MatchType matchType,
        Integer priority,
        MerchantDto merchant,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer matchedDescriptionCount,
        Integer matchedTransactionCount
) {
}
