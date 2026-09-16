package com.mayureshpatel.pfdataservice.dto.merchant;

import java.math.BigDecimal;

/**
 * A single merchant group's spending within the Dashboard's current-period breakdown (PF-841).
 * {@code representativeMerchantId} is only a stable id to key/track by -- once a group spans
 * multiple merchant rows sharing a clean name, there's no single "the" merchant id anymore, so
 * this deliberately does not nest a {@link MerchantDto}.
 *
 * @param representativeMerchantId a stable id for this group ({@code min(id)} of its members)
 * @param displayName              the group's clean name, or its member's original name if unset
 * @param total                    the sum of expense amounts for this group in the current period
 */
public record MerchantBreakdownDto(
        Long representativeMerchantId,
        String displayName,
        BigDecimal total
) {
}
