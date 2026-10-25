package com.mayureshpatel.pfdataservice.dto.report.explore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The explorer's trend: the chosen measure per day, week or month across the period, and, when a
 * comparison is on, the earlier period's buckets lined up by position (the first comparison bucket sits
 * under the first current bucket, and so on).
 *
 * @param current    the shown period's buckets, in order, with empty buckets filled in
 * @param comparison the comparison period's buckets, in order, or empty when not comparing
 */
public record ExploreTrendDto(List<Bucket> current, List<Bucket> comparison) {

    /**
     * One day, week or month of the trend.
     *
     * @param start   the first day of the bucket (a Monday for weeks, the 1st for months)
     * @param value   the measure for the bucket, or null when it has none (a savings rate without income)
     * @param partial whether the bucket is still in progress, so its number is not comparable yet
     */
    public record Bucket(LocalDate start, BigDecimal value, boolean partial) {
    }
}
