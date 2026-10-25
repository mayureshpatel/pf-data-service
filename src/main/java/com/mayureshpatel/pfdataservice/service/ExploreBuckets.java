package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.dto.report.explore.ExploreGroupBy;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * The trend's bucket arithmetic: how coarse the buckets are for a period of a given length, where a
 * bucket starts, and how many buckets a period spans. Weeks start on Monday and months on the 1st, as
 * the database's own truncation does, so bucket starts line up with the grouped rows' keys.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ExploreBuckets {

    private static final long DAILY_UP_TO_DAYS = 31;
    private static final long WEEKLY_UP_TO_DAYS = 92;

    /**
     * @param days the length of the period in days
     * @return day buckets up to 31 days, week buckets up to 92 days, month buckets beyond
     */
    public static ExploreGroupBy granularityFor(long days) {
        if (days <= DAILY_UP_TO_DAYS) {
            return ExploreGroupBy.DAY;
        }
        return days <= WEEKLY_UP_TO_DAYS ? ExploreGroupBy.WEEK : ExploreGroupBy.MONTH;
    }

    /**
     * @param granularity day, week or month
     * @param date        any day
     * @return the first day of the bucket the date falls in
     */
    public static LocalDate bucketStart(ExploreGroupBy granularity, LocalDate date) {
        return switch (granularity) {
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    /**
     * @param granularity day, week or month
     * @param first       the first bucket's start
     * @param steps       how many buckets after the first
     * @return the start of the bucket {@code steps} after the first
     */
    public static LocalDate advance(ExploreGroupBy granularity, LocalDate first, int steps) {
        return switch (granularity) {
            case WEEK -> first.plusWeeks(steps);
            case MONTH -> first.plusMonths(steps);
            default -> first.plusDays(steps);
        };
    }

    /**
     * @param granularity day, week or month
     * @param first       the first day of the period
     * @param last        the last day of the period, inclusive
     * @return how many buckets the period touches
     */
    public static int count(ExploreGroupBy granularity, LocalDate first, LocalDate last) {
        LocalDate start = bucketStart(granularity, first);
        int count = 0;
        while (!start.isAfter(last)) {
            count++;
            start = advance(granularity, bucketStart(granularity, first), count);
        }
        return count;
    }
}
