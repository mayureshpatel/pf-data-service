package com.mayureshpatel.pfdataservice.dto.merchant;

import java.math.BigDecimal;

/**
 * A single merchant's spending within the Dashboard's current-period breakdown (PF-841).
 * {@code representativeMerchantId}/{@code displayName} keep their PF-841 names for wire stability
 * with the not-yet-updated frontend (PF-847) -- under PF-845's deliberate-merchant model these are
 * now simply the merchant's own real {@code id}/{@code name}, not a stand-in for a fragmented
 * group anymore (merchants can no longer fragment, so there's nothing left to represent).
 *
 * @param representativeMerchantId the merchant's id
 * @param displayName              the merchant's name
 * @param total                    the sum of expense amounts for this merchant in the current period
 */
public record MerchantBreakdownDto(
        Long representativeMerchantId,
        String displayName,
        BigDecimal total
) {
}
