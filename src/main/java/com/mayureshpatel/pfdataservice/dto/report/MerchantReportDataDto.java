package com.mayureshpatel.pfdataservice.dto.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single merchant's aggregated spending within a Reports date range (PF-823): total spent,
 * transaction count, and the distinct set of category names those transactions were assigned to.
 * {@code representativeMerchantId}/{@code displayName} keep their PF-841 names for wire stability
 * with the not-yet-updated frontend (PF-847) -- under PF-845's deliberate-merchant model these are
 * now simply the merchant's own real {@code id}/{@code name}, not a stand-in for a fragmented
 * group anymore. Distinct from
 * {@link com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto} (Dashboard's
 * current-month breakdown) since Reports needs both {@code count} and {@code categories}, which
 * Dashboard's DTO never carried.
 *
 * @param representativeMerchantId the merchant's id
 * @param displayName              the merchant's name
 * @param total                    the sum of expense amounts for this merchant in the requested range
 * @param count                    the number of expense transactions summed into {@code total}
 * @param categories               the distinct category names seen across this merchant's
 *                                  transactions in range, excluding uncategorized ones -- never
 *                                  {@code null}, empty if every transaction was uncategorized
 */
public record MerchantReportDataDto(
        Long representativeMerchantId,
        String displayName,
        BigDecimal total,
        long count,
        List<String> categories
) {
}
