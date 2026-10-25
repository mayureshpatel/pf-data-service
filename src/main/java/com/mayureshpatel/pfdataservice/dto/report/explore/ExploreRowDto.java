package com.mayureshpatel.pfdataservice.dto.report.explore;

import java.math.BigDecimal;

/**
 * One row of the explorer's result.
 *
 * @param key             the group's id, a date for time buckets, or null for the "none" group
 * @param label           the group's display name
 * @param value           the measure for this group (null when it has no value, such as a savings rate without income)
 * @param comparisonValue the same measure for the comparison period, or null when not comparing
 * @param count           how many transactions the group holds on the measure's side
 */
public record ExploreRowDto(String key, String label, BigDecimal value, BigDecimal comparisonValue, long count) {
}
