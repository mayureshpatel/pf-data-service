package com.mayureshpatel.pfdataservice.dto.report;

import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;

import java.math.BigDecimal;

/**
 * A single category's aggregated spending within a Reports date range (PF-823): total spent,
 * and the transaction count backing that total. Distinct from {@link com.mayureshpatel.pfdataservice.dto.category.CategoryBreakdownDto}
 * (Dashboard's current-month breakdown) since Reports needs {@code count} to compute an average
 * transaction size, which Dashboard's DTO never carried.
 *
 * @param category the category this total belongs to; {@code null} represents uncategorized spend
 * @param total    the sum of expense amounts for this category in the requested range
 * @param count    the number of expense transactions summed into {@code total}
 */
public record CategoryReportDataDto(
        CategoryDto category,
        BigDecimal total,
        long count
) {
}
