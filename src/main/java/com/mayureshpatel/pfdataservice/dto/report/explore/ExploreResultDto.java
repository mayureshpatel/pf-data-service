package com.mayureshpatel.pfdataservice.dto.report.explore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The explorer's answer for one measure, grouping and set of filters.
 *
 * @param rows            the groups, largest value first
 * @param total           the measure over the whole period (correct even when a transaction appears in several groups)
 * @param comparisonTotal the measure over the comparison period, or null
 * @param totalCount      how many transactions the total covers
 * @param excludedCount   how many transfers and adjustments were left out
 * @param comparisonStart the first day of the comparison period, or null
 * @param comparisonEnd   the last day of the comparison period, or null
 * @param comparisonLabel a readable description of the comparison with its real dates, or null
 * @param inProgress      whether the requested range runs past today (so it was clamped to today)
 */
public record ExploreResultDto(
        List<ExploreRowDto> rows,
        BigDecimal total,
        BigDecimal comparisonTotal,
        long totalCount,
        long excludedCount,
        LocalDate comparisonStart,
        LocalDate comparisonEnd,
        String comparisonLabel,
        boolean inProgress
) {
}
