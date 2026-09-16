package com.mayureshpatel.pfdataservice.dto.report;

import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single merchant's aggregated spending within a Reports date range (PF-823): total spent,
 * transaction count, and the distinct set of category names those transactions were assigned to.
 * Distinct from {@link com.mayureshpatel.pfdataservice.dto.merchant.MerchantBreakdownDto}
 * (Dashboard's current-month breakdown) since Reports needs both {@code count} and
 * {@code categories}, which Dashboard's DTO never carried.
 *
 * @param merchant   the merchant this total belongs to
 * @param total      the sum of expense amounts for this merchant in the requested range
 * @param count      the number of expense transactions summed into {@code total}
 * @param categories the distinct category names seen across this merchant's transactions in
 *                    range, excluding uncategorized ones -- never {@code null}, empty if every
 *                    transaction was uncategorized
 */
public record MerchantReportDataDto(
        MerchantDto merchant,
        BigDecimal total,
        long count,
        List<String> categories
) {
}
