package com.mayureshpatel.pfdataservice.dto.report;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single merchant group's aggregated spending within a Reports date range (PF-823): total
 * spent, transaction count, and the distinct set of category names those transactions were
 * assigned to. {@code representativeMerchantId} is only a stable id to key/track by -- once a
 * group spans multiple merchant rows sharing a clean name (PF-841), there's no single "the"
 * merchant id anymore, so this deliberately does not nest a
 * {@link com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto}. Distinct from
 * {@link com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto} (Dashboard's
 * current-month breakdown) since Reports needs both {@code count} and {@code categories}, which
 * Dashboard's DTO never carried.
 *
 * @param representativeMerchantId a stable id for this group ({@code min(id)} of its members)
 * @param displayName              the group's clean name, or its member's original name if unset
 * @param total                    the sum of expense amounts for this group in the requested range
 * @param count                    the number of expense transactions summed into {@code total}
 * @param categories               the distinct category names seen across this group's transactions
 *                                  in range, excluding uncategorized ones -- never {@code null},
 *                                  empty if every transaction was uncategorized
 */
public record MerchantReportDataDto(
        Long representativeMerchantId,
        String displayName,
        BigDecimal total,
        long count,
        List<String> categories
) {
}
